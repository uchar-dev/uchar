package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.State;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Initialisations on the exec-model branch (RUNTIME-SEMANTICS ch. 5.4: the guard reads the input, not the
 * initialisation's own write). The first document has the shape of the golden {@code jutterswet-initialisatievolgorde},
 * on whole numbers: "Het bergloon van een Vondst moet geïnitialiseerd worden op 2 maal zijn berekend gewicht indien zijn
 * gewicht groter is dan 10", "Het vindloon van een Vondst moet geïnitialiseerd worden op zijn berekend gewicht plus 5"
 * and "Het berekend gewicht van een Vondst moet berekend worden als zijn gewicht plus 1". V1 (gewicht 12): bergloon 26,
 * vindloon 18. V2 (gewicht 8): bergloon leeg, vindloon 14. V3 (gewicht 12, bergloon 40 and vindloon 3 as input): both
 * inputs stand. V4 (gewicht 30, vindloon 7 as input): bergloon 62, vindloon 7. An initialisation that fires before the
 * berekend gewicht is derived reads its input standing, leeg; its own write does not block it, so truth maintenance
 * withdraws that write when the standing changes and the initialisation fires again on the derived value. Every order of
 * the three rules in the document, and every order of the five generated rules, at least one of which must fire an
 * initialisation on that input standing.
 *
 * <p>The second document has a computed value displace an initialisation on the same target: "Een Vondst is
 * geregistreerd indien hij gemeld is", "Een Vondst is onbeheerd indien hij niet geregistreerd is", "Het bergloon van een
 * Vondst moet gesteld worden op zijn taxatie, indien hij onbeheerd is" and "Het bergloon van een Vondst moet
 * geïnitialiseerd worden op 7". V1 (gemeld, taxatie 30): 7. V2 (taxatie 30): 30, whichever fires first. V3 (nothing):
 * the computation is leeg and writes nothing, so the initialisation fills the target (ch. 7.3), 7. V4 (gemeld, bergloon 5
 * as input): 5. V5 (taxatie 30, bergloon 5 as input): the computation decides and replaces the input (ch. 7.8), 30.
 * Every order of the four rules in the document, and a seeded sample of orders of the generated rules, at least one of
 * which must fire the initialisation on V2 before the computation displaces it.
 * A division by leeg in an initialisation's value deposits only where the initialisation applies, and two
 * initialisations on one target are refused by name (IR-SPEC WF-079). The reference interpreter's runner passed the
 * cases of the first two documents as well (measured 2026-10-08).
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's second step, whose manifest refused every
 * initialisation): every row on this branch was refused, "tms-transpiler SKIP: regel: standaardbergloon — een
 * initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet", and the refusal row
 * got that sentence instead of its own, "expected: &lt;tms-transpiler SKIP: regel: standaardbergloon — twee of meer
 * initialisaties op één doel …&gt; but was: &lt;tms-transpiler SKIP: regel: standaardbergloon — een initialisatie (van
 * 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet&gt;". The rows on the exec-model branch
 * were green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). (1) The naive guard, no {@code Waarde} of any
 * writer on the target, so the initialisation's own write counts: the replay reached the fire cap in the document
 * orders, "volgorde [standaardbergloon, standaardvindloon, berekend gewicht van een vondst] ==&gt; expected: &lt;null&gt;
 * but was: &lt;case: geen fixpunt (kringverwijzing?)&gt;", in the displacement document, "volgorde [registratie,
 * onbeheerd, bergloon, standaardbergloon] ==&gt; expected: &lt;null&gt; but was: &lt;case: geen fixpunt
 * (kringverwijzing?)&gt;", and in the first generated order, "geen fixpunt in volgorde [standaardbergloon,
 * standaardvindloon, berekend gewicht van een vondst, stand Vondst.berekend gewicht: uit een afleiding, stand
 * Vondst.berekend gewicht: uit de invoer]"; on the single Vondst of the golden's first case both targets ended leeg
 * without a message, as A5 of the trial of card drools-logische-afleiding measured, "case1-beide-geschreven.json:
 * attribuut bergloon: verwacht 26.00, werkelijk leeg; attribuut vindloon: verwacht 18.00, werkelijk leeg"
 * ({@link TmsTrialSetWitnessTest}). (2) Without the guard's read of the input on the bean: "volgorde
 * [standaardbergloon, standaardvindloon, berekend gewicht van een vondst] ==&gt; expected: &lt;null&gt; but was:
 * &lt;case: attribuut bergloon: verwacht 40, werkelijk 26; attribuut vindloon: verwacht 3, werkelijk 18; attribuut
 * vindloon: verwacht 7, werkelijk 36&gt;", and the deposit row, "V2 ==&gt; expected: &lt;null&gt; but was: &lt;case:
 * verwacht consistent, werkelijk inconsistent (standaardaandeel @ V2)&gt;". (3) Without the other writers'
 * {@code Waarde} in the guard, only the displacement rows failed, at the write-back: "geen fixpunt: twee schrijvers van
 * één doel geven verschillende waarden — de regel "bergloon" schrijft 30 en de regel "standaardbergloon" schrijft 7 in
 * het attribuut bergloon van een Vondst (Vondst@…); divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)".
 */
class TmsInitialisationWitnessTest {

    private static final String COMPUTED = ownRead("berekend gewicht", "Vondst");

    private static final List<String> ORDER = List.of(
            computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", binary("times", number("2"), COMPUTED),
                    comparison("greater", ownRead("gewicht", "Vondst"), number("10"))),
            computingRule("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon", binary("plus", COMPUTED, number("5")),
                    "null"),
            computingRule("berekend gewicht van een vondst", "BEREKENING", "Vondst", "berekend gewicht",
                    binary("plus", ownRead("gewicht", "Vondst"), number("1")), "null"));

    private static final String ORDER_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("gewicht", WHOLE), attribute("berekend gewicht", WHOLE), attribute("bergloon", WHOLE),
                    attribute("vindloon", WHOLE)), List.of())), List.of());

    private static final List<String> DISPLACEMENT = List.of(
            characteristicRule("registratie", "Vondst", "geregistreerd", check("gemeld", false)),
            characteristicRule("onbeheerd", "Vondst", "onbeheerd", check("geregistreerd", true)),
            computingRule("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", ownRead("taxatie", "Vondst"),
                    check("onbeheerd", false)),
            computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", number("7"), "null"));

    private static final String DISPLACEMENT_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("taxatie", WHOLE), attribute("bergloon", WHOLE)),
            List.of("gemeld", "geregistreerd", "onbeheerd"))), List.of());

    // ---------------- the shape of jutterswet-initialisatievolgorde ----------------

    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(ORDER)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(ORDER_MODEL, order), orderCase(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    /** Three rules and the two standing rules of the berekend gewicht, which both initialisations read; gewicht is
     *  input and read off the bean. Five rules have 120 orders, so every one is tried. */
    @Test void every_order_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(ORDER_MODEL, ORDER),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(5, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        int orders = 0, onTheInputState = 0;
        for (List<Rule> order : TmsDocuments.permutations(generated)) {
            if (fireOrder(b, order)) onTheInputState++;
            orders++;
        }
        assertEquals(120, orders);
        assertTrue(onTheInputState > 0, "geen volgorde vuurde een initialisatie op de invoerstand van V1");
    }

    /** Fires V1 to V4 with the rules in {@code order}, checks the beans after the write-back, and says whether an
     *  initialisation fired on V1 while the berekend gewicht stood at its input, leeg: the transient path. */
    private static boolean fireOrder(DroolsBackend.Build b, List<Rule> order) throws Exception {
        List<String> names = order.stream().map(Rule::getName).toList();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = TmsDocuments.kieBase(b, order).newKieSession();
        try {
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "12"), List.of());
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "8"), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst",
                    Map.of("gewicht", "12", "bergloon", "40", "vindloon", "3"), List.of());
            Object v4 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "30", "vindloon", "7"), List.of());
            boolean[] intermediateState = {false};
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    List<?> facts = e.getMatch().getObjects();
                    if (e.getMatch().getRule().getName().startsWith("standaard") && facts.stream().anyMatch(o -> o == v1)
                            && facts.stream().anyMatch(o -> o instanceof State s && s.value() == null))
                        intermediateState[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3, v4)) session.insert(v);
            assertNull(DroolsBackend.fire(b, session, 100_000).noFixpoint(), () -> "geen fixpunt in volgorde " + names);
            DroolsBackend.setDerivedValues(b, session);
            String where = " in volgorde " + names;
            endsOn(v1, "berekend gewicht", "13", "V1" + where);
            endsOn(v1, "bergloon", "26", "V1" + where);
            endsOn(v1, "vindloon", "18", "V1" + where);
            endsOn(v2, "bergloon", null, "V2" + where);
            endsOn(v2, "vindloon", "14", "V2" + where);
            endsOn(v3, "bergloon", "40", "V3" + where);
            endsOn(v3, "vindloon", "3", "V3" + where);
            endsOn(v4, "bergloon", "62", "V4" + where);
            endsOn(v4, "vindloon", "7", "V4" + where);
            return intermediateState[0];
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static JsonNode orderCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("gewicht", "8"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of("gewicht", "12", "bergloon", "40", "vindloon", "3"), List.of(),
                                Map.of()),
                        instance("V4", "Vondst", Map.of("gewicht", "30", "vindloon", "7"), List.of(), Map.of())),
                List.of(value("V1", "berekend gewicht", "13"), value("V1", "bergloon", "26"),
                        value("V1", "vindloon", "18"), value("V2", "berekend gewicht", "9"),
                        value("V2", "bergloon", null), value("V2", "vindloon", "14"), value("V3", "bergloon", "40"),
                        value("V3", "vindloon", "3"), value("V4", "bergloon", "62"), value("V4", "vindloon", "7"),
                        consistent()));
    }

    // ---------------- a computed value displaces the initialisation ----------------

    @Test void a_computed_value_displaces_the_initialisation_in_every_order_of_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(DISPLACEMENT)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(DISPLACEMENT_MODEL, order), displacementCase(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    /** Four rules and the characteristic standings of geregistreerd and onbeheerd (three rules each); taxatie is input
     *  and nothing reads the bergloon, so it has no standing. */
    @Test void sample_of_orders_of_the_generated_rules_at_displacement() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(DISPLACEMENT_MODEL, DISPLACEMENT),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(10, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        int displaced = 0;
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100))
            if (fireDisplacement(b, order)) displaced++;
        assertTrue(displaced > 0, "geen volgorde vuurde de initialisatie op V2 voordat de berekening haar verdrong");
    }

    /** Fires V1 to V5 with the rules in {@code order}, checks the beans after the write-back, and says whether the
     *  initialisation fired on V2, whose bergloon the computation decides: that write was displaced. */
    private static boolean fireDisplacement(DroolsBackend.Build b, List<Rule> order) throws Exception {
        List<String> names = order.stream().map(Rule::getName).toList();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = TmsDocuments.kieBase(b, order).newKieSession();
        try {
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of());
            Object v4 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("bergloon", "5"), List.of("gemeld"));
            Object v5 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "bergloon", "5"), List.of());
            boolean[] displaced = {false};
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("standaardbergloon")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v2))
                        displaced[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3, v4, v5)) session.insert(v);
            assertNull(DroolsBackend.fire(b, session, 100_000).noFixpoint(), () -> "geen fixpunt in volgorde " + names);
            DroolsBackend.setDerivedValues(b, session);
            String where = " in volgorde " + names;
            endsOn(v1, "bergloon", "7", "V1" + where);
            endsOn(v2, "bergloon", "30", "V2" + where);
            endsOn(v3, "bergloon", "7", "V3" + where);
            endsOn(v4, "bergloon", "5", "V4" + where);
            endsOn(v5, "bergloon", "30", "V5" + where);
            return displaced[0];
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static JsonNode displacementCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "30"), List.of("gemeld"), Map.of()),
                        instance("V2", "Vondst", Map.of("taxatie", "30"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of(), List.of(), Map.of()),
                        instance("V4", "Vondst", Map.of("bergloon", "5"), List.of("gemeld"), Map.of()),
                        instance("V5", "Vondst", Map.of("taxatie", "30", "bergloon", "5"), List.of(), Map.of())),
                List.of(value("V1", "bergloon", "7"), value("V2", "bergloon", "30"), value("V3", "bergloon", "7"),
                        value("V4", "bergloon", "5"), value("V5", "bergloon", "30"), consistent()));
    }

    // ---------------- the §6.5 deposit under the guard ----------------

    /** "Het aandeel van een Vondst moet geïnitialiseerd worden op zijn opbrengst gedeeld door zijn deelnemers". V1
     *  (opbrengst 100, no deelnemers): aandeel leeg and the deposit, under the initialisation's name. V2 (the same,
     *  with aandeel 5 as input): the initialisation does not apply, so it evaluates nothing and deposits nothing; the
     *  reference interpreter tests the guard before it evaluates the value, and its runner passed both cases on this
     *  document (measured 2026-10-08). */
    @Test void a_division_by_empty_deposits_only_where_the_initialisation_applies() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
                List.of(attribute("opbrengst", WHOLE), attribute("deelnemers", WHOLE), attribute("aandeel", WHOLE)),
                List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of(computingRule("standaardaandeel", "INITIALISATIE", "Vondst",
                "aandeel", binary("dividedBy", ownRead("opbrengst", "Vondst"), ownRead("deelnemers", "Vondst")), "null")));
        JsonNode applies = TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("opbrengst", "100"), List.of(), Map.of())),
                List.of(value("V1", "aandeel", null), TmsDocuments.inconsistent("standaardaandeel", "V1")));
        JsonNode input = TmsDocuments.caseDocument(
                List.of(instance("V2", "Vondst", Map.of("opbrengst", "100", "aandeel", "5"), List.of(), Map.of())),
                List.of(value("V2", "aandeel", "5"), consistent()));
        assertNull(CaseRunner.playCase(ir, applies, DroolsBackend.Branch.EXEC_MODEL), "V1");
        assertNull(CaseRunner.playCase(ir, input, DroolsBackend.Branch.EXEC_MODEL), "V2");
    }

    // ---------------- two initialisations on one target ----------------

    /** Where both apply nothing ranks them, conditional or not (RUNTIME-SEMANTICS ch. 5.4). The backend runs no
     *  verifier, so the branch refuses the pair itself, at the first of them in the document. */
    @Test void two_initialisations_on_a_target_are_refused() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
                List.of(attribute("gewicht", WHOLE), attribute("bergloon", WHOLE)), List.of())), List.of());
        Path ir = TmsDocuments.document(model, List.of(
                computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", number("7"), "null"),
                computingRule("zwaar bergloon", "INITIALISATIE", "Vondst", "bergloon", number("9"),
                        comparison("greater", ownRead("gewicht", "Vondst"), number("100")))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: standaardbergloon — twee of meer initialisaties op één doel ('bergloon'"
                + " van Vondst: \"standaardbergloon\", \"zwaar bergloon\"), voorwaardelijk of niet: waar ze allebei gelden"
                + " rangschikt niets ze en bepaalt het fixpunt de waarde niet (RUNTIME-SEMANTICS ch. 5.4, WF-079)",
                e.getMessage());
    }

    // ---------------- a division in an initialisation beside another writer ----------------

    private static final String DIVISION_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("taxatie", WHOLE), attribute("bergloon", WHOLE), attribute("opbrengst", WHOLE),
                    attribute("deelnemers", WHOLE)),
            List.of("gemeld", "erkend"))), List.of());

    private static final List<String> DIVISION = List.of(
            characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false)),
            computingRule("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", ownRead("taxatie", "Vondst"),
                    check("erkend", false)),
            computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon",
                    binary("dividedBy", ownRead("opbrengst", "Vondst"), ownRead("deelnemers", "Vondst")), "null"));

    private static final String DIVISION_REFUSAL = "transpiler SKIP: regel: standaardbergloon — een deling in een"
            + " initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'bergloon' van Vondst wordt ook"
            + " geschreven door \"bergloon\"): naast een andere schrijver is een initialisatie de laagste default"
            + " (RUNTIME-SEMANTICS ch. 5.4, 7.7), en deze tak rekent haar uit zodra wat zij leest staat, ook waar de"
            + " andere schrijver haar later verdringt; een deling door nul daar is een fout op een tussenstand, die deze"
            + " tak intrekt zodra de andere schrijver beslist, waar de referentie afhankelijk van de volgorde van de"
            + " regels een fout of de waarde geeft: of zo'n fout telt, wacht op een normbesluit (ch. 5.2, 7.1)";

    /**
     * "Een Vondst is erkend indien hij gemeld is", "Het bergloon van een Vondst moet gesteld worden op zijn taxatie,
     * indien hij erkend is" and "Het bergloon van een Vondst moet geïnitialiseerd worden op zijn opbrengst gedeeld door
     * zijn deelnemers". Beside the computation the initialisation is the lowest default (RUNTIME-SEMANTICS ch. 7.7), and
     * only the deciding default is evaluated (ch. 7.1 and 7.5): V1 (gemeld, taxatie 30, opbrengst 100, deelnemers 0) ends
     * at 30, consistent, and the division by zero is never reached. This branch evaluates the initialisation as soon as
     * what it reads stands, before the computation's condition holds through the derived {@code erkend}, so it refuses
     * the division there, in every order of the document. An initialisation with a division and no other writer is not
     * speculative and stays carried ({@link #a_division_by_empty_deposits_only_where_the_initialisation_applies}).
     *
     * <p><b>Red before the refusal</b> (measured 2026-10-08, the division carried): every order of the document played
     * into the fault, the first "[erkenning, bergloon, standaardbergloon] DelingDoorNul: deling door nul in de regel
     * "standaardbergloon": 100 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS
     * ch. 1.4, punt 4)", and the other five the same.
     *
     * <p>Since the evaluation fault is a fact that counts at the fixpoint ({@link TmsFaultWitnessTest}), the fault there is
     * withdrawn when the computation displaces the initialisation, and with the refusal switched off every order played
     * bergloon 30 (measured 2026-10-08, not committed). The reference interpreter gives that value in one order of the
     * rules and the fault in the other, so whether a fault on an intermediate state counts waits on a decision of the
     * norm, and the refusal stays until then; its reason says so.
     */
    @Test void a_division_in_an_initialisation_beside_another_writer_is_refused() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "30", "opbrengst", "100", "deelnemers", "0"),
                        List.of("gemeld"), Map.of())),
                List.of(value("V1", "bergloon", "30"), consistent()));
        List<String> expected = new ArrayList<>(), outcomes = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(DIVISION)) {
            List<String> names = TmsDocuments.ruleNames(order);
            String outcome;
            try {
                outcome = "gespeeld: " + CaseRunner.playCase(TmsDocuments.document(DIVISION_MODEL, order), caseDocument,
                        DroolsBackend.Branch.EXEC_MODEL);
            } catch (RuntimeException e) {
                outcome = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            expected.add(names + " Excluded: " + DIVISION_REFUSAL);
            outcomes.add(names + " " + outcome);
        }
        assertEquals(6, outcomes.size());
        assertEquals(expected, outcomes);
    }

    // ---------------- helpers ----------------

    /** Attribute {@code attribute} of the bean ends at the number {@code lexeme}, or leeg where it is null. */
    private static void endsOn(Object beanObject, String attribute, String lexeme, String where) throws Exception {
        Object v = TmsDocuments.read(beanObject, attribute);
        if (lexeme == null) assertNull(v, attribute + " van " + where);
        else assertTrue(v instanceof BigDecimal d && new BigDecimal(lexeme).compareTo(d) == 0,
                () -> attribute + " van " + where + ": verwacht " + lexeme + ", werkelijk " + v);
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
