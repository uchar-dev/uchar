package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Fault;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two or more writers of one target on the exec-model branch (RUNTIME-SEMANTICS ch. 5.7, "Divergence by two
 * writers of one target"; ch. 1.2g and 7.8 for a computed leeg; ch. 7.10 per concrete type).
 *
 * <p>The first document has three computing rules on the doel of a Vondst beside the rule that derives what two of
 * them read: "De tussen van een Vondst moet berekend worden als zijn a plus 1", "doel volgens tussen" (zijn tussen,
 * indien zijn tussen groter is dan 10), "doel volgens b" (zijn b, indien zijn b gevuld is) and "doel volgens c" (zijn
 * c, indien zijn tussen kleiner is dan 5). The doel has three writers, so it lies outside the standing domain of ch.
 * 7.8, and a writer that fires with a leeg value writes leeg (ch. 1.2g). V1 (a 20): one writer, doel 21. V2 (a 1, doel
 * 99 as input): "doel volgens c" fires with a leeg c and erases the input, doel leeg. V3 (a 20, b 21): two writers with
 * the same value, doel 21. V4 (a 7, b 7): the conditions of the first and the third writer exclude each other and
 * neither holds, doel 7. V5 (a 7, tussen 3 as input, b 5, c 9): on the input standing of the tussen "doel volgens c"
 * holds and writes 9 beside the 5 of "doel volgens b", a difference on an intermediate state only; at the fixpoint the
 * tussen is 8, and the doel 5. V6 (a 20, b 5): 21 against 5, the fault of ch. 5.7. V7 (a 1, b 7): 7 against a leeg
 * that "doel volgens c" writes, the fault as well. Every order of the four rules in the document, where the fault
 * names its writers in the order of that document; and a seeded sample of orders of the generated rules, at least one
 * of which must put the fault of V5 in the session on the intermediate state, from which truth maintenance withdraws
 * it.
 *
 * <p>The second document has the doel written on two sister subtypes and on their supertype: "doel van drijfgoed" (zijn
 * a), on Drijfgoed, "doel van wrakgoed" (zijn a plus 10), on Wrakgoed, "doel van een grote vondst" (5, indien zijn a
 * groter is dan 100), on Vondst, and "a van een vondst" (zijn c plus 1). Read per concrete type (IR-SPEC WF-079, ch.
 * 7.10) the two sister rules never apply to one instance, while the supertype's rule applies to every instance of
 * both. D1 (Drijfgoed, c 1): 2. W1 (Wrakgoed, c 1): 12. V1 (Vondst, c 1, doel 4 as input): no writer fires and the
 * input stands. V2 (Vondst, c 200): 5. D2 (Drijfgoed, c 200): 201 against 5, the fault. Every order of the four rules.
 *
 * <p>The third document has a computing rule beside a conditional initialisation, which the desugaring of ch. 7.7 does
 * not take, so the rule is outside the standing domain: "doel volgens b" (zijn b, indien zijn tussen groter is dan 10),
 * "standaarddoel" (geïnitialiseerd op 3, indien zijn c gevuld is) and the rule of the tussen. The guard of an
 * initialisation reads the target as the other writers leave it (ch. 5.4): a value another writer places switches it
 * off, and a leeg another writer writes leaves the target empty, input or not, so the initialisation fills it, and the
 * two writers then demand different values. V1 (a 20, b 7, c 1, tussen 3 as input): the initialisation fills the doel
 * on the intermediate state, and the rule's 7 displaces it at the fixpoint. V2 (a 20, doel 9 as input): the rule writes
 * leeg and erases the input; the initialisation's condition does not hold: leeg. V3 (a 1, c 1): only the
 * initialisation, 3. V4 (a 1, c 1, doel 9 as input): the input stands. V5 (a 20, c 1) and V6 (the same with doel 9 as
 * input): the rule writes leeg and the initialisation 3, the fault. Every order of the three rules, and a seeded sample
 * of orders of the generated rules.
 *
 * <p>Two initialisations on sister subtypes play, and one on their supertype beside them is refused (WF-079 per
 * concrete type). A rule on a member an object creation sets: {@code TmsCreationInputWitnessTest}.
 *
 * <p>The red case {@code corpus/red/jutterswet-dubbele-melding} gives on this branch the fault its
 * {@code expectedFault} pins (EF-016 and its parameters), the writers in the order of the document, as the reference
 * interpreter names them.
 *
 * <p><b>Measured in the reference interpreter and the ASP backend</b> (2026-10-09, on these documents in every order
 * of their rules, written out with their cases and run through {@code ref/bin/runner.exe} and
 * {@code backends/clingo/bin/clingo_runner.exe}). ref passed the cases that play in every order (6, 4 and 4
 * expectations) and faulted every case that faults here, in the sentence "two writers of one target disagree: rule
 * "doel volgens tussen" writes 21 and rule "doel volgens b" writes 5 to the attribute doel of V6, a Vondst", with the
 * writers in the order of the document in all 84 faulting runs and a leeg written as "leeg". clingo passed the second document's cases, faulted every case that faults here (UNSAT on its named
 * {@code viol} witness), and refused at translation the cases of the first and the third document that play, since
 * they put input on a target outside the standing domain ("the case supplies input for computed target "doel": ref
 * (like v0) overwrites it, which has no monotone encoding here"). On the red case ref says "two writers of one target
 * disagree: rule "melding op vinddag" writes 2026-03-04 and rule "melding op aanspoeldag" writes 2026-03-01 to the
 * attribute meldingsdatum of V, a Vondst". The document of the two initialisations on sister subtypes passed in ref
 * and in clingo in all six orders.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-09 on {@code f7bb0dd4}): the first and the second document were
 * refused, "tms-transpiler SKIP: regel: doel volgens tussen — twee of meer rekenende regels op één doel ('doel' van
 * Vondst: "doel volgens tussen", "doel volgens b", "doel volgens c"): hun v0-betekenis is de volgorde op de agenda
 * (RUNTIME-SEMANTICS ch. 7.7, WF-079)", the second with the writers "doel van drijfgoed", "doel van wrakgoed" and "doel
 * van een grote vondst"; the third, "tms-transpiler SKIP: regel: doel volgens b — een rekenende regel op een doel
 * buiten het staande domein ('doel' van Vondst), waar een berekende leeg het doel leeg maakt (RUNTIME-SEMANTICS ch.
 * 7.8): de truth-maintenance-tak draagt deze vorm nog niet"; the red case gave that refusal for its two rules instead of
 * the fault; the initialisations on sister subtypes were refused, "twee of meer initialisaties op één doel ('doel' van
 * Vondst: "standaarddoel van drijfgoed", "standaarddoel van wrakgoed"), voorwaardelijk of niet: …". Only the row on
 * the exec-model branch was green.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). (1) The guard of the initialisation that does not
 * count a written leeg: V5 of the third document played, "Expected java.lang.RuntimeException to be thrown, but nothing
 * was thrown", in the document orders and in the generated ones. (2) A computed leeg that writes nothing outside the
 * standing domain: "volgorde [tussen van een vondst, doel volgens tussen, doel volgens b, doel volgens c] ==&gt;
 * expected: &lt;null&gt; but was: &lt;case: verwacht leeg, werkelijk 99&gt;", and four of the seven tests red. (3)
 * Without the rule that compares the writers' values, the write-back's own check gives the same sentence at the
 * fixpoint, and only the count of the generated rules tells: "expected: &lt;7&gt; but was: &lt;6&gt;".
 */
class TmsWritersWitnessTest {

    private static final String A = ownRead("a", "Vondst");
    private static final String B = ownRead("b", "Vondst");
    private static final String C = ownRead("c", "Vondst");
    private static final String BETWEEN = ownRead("tussen", "Vondst");

    private static final String BETWEEN_RULE = computingRule("tussen van een vondst", "BEREKENING", "Vondst", "tussen",
            binary("plus", A, number("1")), "null");

    // ---------------- three computing rules on one target ----------------

    private static final List<String> THREE = List.of(BETWEEN_RULE,
            computingRule("doel volgens tussen", "GELIJKSTELLING", "Vondst", "doel", BETWEEN,
                    comparison("greater", BETWEEN, number("10"))),
            computingRule("doel volgens b", "GELIJKSTELLING", "Vondst", "doel", B, predicate("filled", B)),
            computingRule("doel volgens c", "GELIJKSTELLING", "Vondst", "doel", C, comparison("less", BETWEEN, number("5"))));

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("a", WHOLE), attribute("b", WHOLE), attribute("c", WHOLE), attribute("tussen", WHOLE),
                    attribute("doel", WHOLE)), List.of())), List.of());

    @Test void three_writers_in_every_order_of_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(THREE)) {
            Path ir = TmsDocuments.document(MODEL, order);
            List<String> names = TmsDocuments.ruleNames(order);
            assertNull(CaseRunner.playCase(ir, threeCase(), DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            assertEquals(conflict(names, "doel volgens tussen", "21", "doel volgens b", "5", "doel", "V6, een Vondst"),
                    fault(ir, caseOf("V6", "Vondst", Map.of("a", "20", "b", "5"))),
                    () -> "volgorde " + names);
            assertEquals(conflict(names, "doel volgens b", "7", "doel volgens c", "leeg", "doel", "V7, een Vondst"),
                    fault(ir, caseOf("V7", "Vondst", Map.of("a", "1", "b", "7"))),
                    () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    /** The rule of the tussen, its two standing rules, the three writers and the rule that compares their values: a
     *  sample of their orders, each over V1 to V5 and then over V6, written back without a case label. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, THREE),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(7, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        int onTheIntermediateState = 0;
        for (List<Rule> order : TmsDocuments.sample(generated, 20261009L, 60)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = beanObject(b, Map.of("a", "20")), v2 = beanObject(b, Map.of("a", "1", "doel", "99")),
                    v3 = beanObject(b, Map.of("a", "20", "b", "21")), v4 = beanObject(b, Map.of("a", "7", "b", "7")),
                    v5 = beanObject(b, Map.of("a", "7", "tussen", "3", "b", "5", "c", "9"));
            List<Object> conflictOpV5 = new ArrayList<>();
            TmsDocuments.fire(b, order, List.of(v1, v2, v3, v4, v5), s -> s.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Fault f && f.onderwerp() == v5
                                    && f.fault().getClass().getSimpleName().equals("WritersConflict"))
                                conflictOpV5.add(f);
                        }
                    }), s -> assertTrue(s.getObjects(o -> o instanceof Fault).isEmpty(), "een fout aan het fixpunt"));
            String where = " in volgorde " + names;
            assertNumber("21", v1, "doel", "V1" + where);
            assertNumber(null, v2, "doel", "V2" + where);
            assertNumber("21", v3, "doel", "V3" + where);
            assertNumber("7", v4, "doel", "V4" + where);
            assertNumber("5", v5, "doel", "V5" + where);
            assertNumber("8", v5, "tussen", "V5" + where);
            if (!conflictOpV5.isEmpty()) onTheIntermediateState++;
            Object v6 = beanObject(b, Map.of("a", "20", "b", "5"));
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> TmsDocuments.fire(b, order, List.of(v6), s -> {}, s -> {}), () -> "V6" + where);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"doel volgens"
                    + " tussen\" schrijft 21 en de regel \"doel volgens b\" schrijft 5 in het attribuut doel van een Vondst"
                    + " (Vondst@" + Integer.toHexString(System.identityHashCode(v6)) + "); divergentie is een fout"
                    + " (RUNTIME-SEMANTICS ch. 5.7)", e.getMessage(), "V6" + where);
        }
        assertTrue(onTheIntermediateState > 0, "geen volgorde zette de fout van V5 op de tussenstand in de sessie");
    }

    private static JsonNode threeCase() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("a", "20"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("a", "1", "doel", "99"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of("a", "20", "b", "21"), List.of(), Map.of()),
                        instance("V4", "Vondst", Map.of("a", "7", "b", "7"), List.of(), Map.of()),
                        instance("V5", "Vondst", Map.of("a", "7", "tussen", "3", "b", "5", "c", "9"), List.of(),
                                Map.of())),
                List.of(value("V1", "doel", "21"), value("V2", "doel", null), value("V3", "doel", "21"),
                        value("V4", "doel", "7"), value("V5", "doel", "5"), value("V5", "tussen", "8")));
    }

    // ---------------- writers on sister subtypes and their supertype ----------------

    private static final List<String> SISTERS = List.of(
            computingRule("doel van drijfgoed", "GELIJKSTELLING", "Drijfgoed", "doel", ownRead("a", "Drijfgoed"), "null"),
            computingRule("doel van wrakgoed", "GELIJKSTELLING", "Wrakgoed", "doel",
                    binary("plus", ownRead("a", "Wrakgoed"), number("10")), "null"),
            computingRule("doel van een grote vondst", "GELIJKSTELLING", "Vondst", "doel", number("5"),
                    comparison("greater", A, number("100"))),
            computingRule("a van een vondst", "BEREKENING", "Vondst", "a", binary("plus", C, number("1")), "null"));

    private static final String SISTER_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Vondst", List.of(attribute("a", WHOLE), attribute("c", WHOLE),
                    attribute("doel", WHOLE)), List.of()),
            TmsDocuments.subtype("Drijfgoed", "Vondst", List.of(), List.of()),
            TmsDocuments.subtype("Wrakgoed", "Vondst", List.of(), List.of())), List.of());

    @Test void writers_on_sister_subtypes_in_every_order_of_the_document() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("D1", "Drijfgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instance("W1", "Wrakgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instance("V1", "Vondst", Map.of("c", "1", "doel", "4"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("c", "200"), List.of(), Map.of())),
                List.of(value("D1", "doel", "2"), value("W1", "doel", "12"), value("V1", "doel", "4"),
                        value("V2", "doel", "5")));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(SISTERS)) {
            Path ir = TmsDocuments.document(SISTER_MODEL, order);
            List<String> names = TmsDocuments.ruleNames(order);
            assertNull(CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            assertEquals(conflict(names, "doel van drijfgoed", "201", "doel van een grote vondst", "5", "doel",
                            "D2, een Drijfgoed"),
                    fault(ir, caseOf("D2", "Drijfgoed", Map.of("c", "200"))),
                    () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    // ---------------- initialisations on sister subtypes ----------------

    private static final List<String> SISTER_INITIALISATIONS = List.of(
            computingRule("standaarddoel van drijfgoed", "INITIALISATIE", "Drijfgoed", "doel", ownRead("a", "Drijfgoed"),
                    "null"),
            computingRule("standaarddoel van wrakgoed", "INITIALISATIE", "Wrakgoed", "doel", number("7"), "null"),
            computingRule("a van een vondst", "BEREKENING", "Vondst", "a", binary("plus", C, number("1")), "null"));

    /** "Het doel van een Drijfgoed moet geïnitialiseerd worden op zijn a", "Het doel van een Wrakgoed moet
     *  geïnitialiseerd worden op 7" and the rule of the a: two initialisations on sister subtypes never apply to one
     *  instance (IR-SPEC WF-079, its initialisation arm, read per concrete type). D1 (Drijfgoed, c 1): 2, the a derived
     *  after the initialisation may have read its input standing. W1 (Wrakgoed): 7. D2 (Drijfgoed, doel 5 as input): 5.
     *  V1 (Vondst): no initialisation, leeg. Every order of the three rules. An initialisation on the supertype beside
     *  them writes every instance of both, and is refused with the one on the subtype that comes first. */
    @Test void initialisations_on_sister_subtypes_in_every_order_of_the_document() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("D1", "Drijfgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instance("W1", "Wrakgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instance("D2", "Drijfgoed", Map.of("c", "1", "doel", "5"), List.of(), Map.of()),
                        instance("V1", "Vondst", Map.of("c", "1"), List.of(), Map.of())),
                List.of(value("D1", "doel", "2"), value("W1", "doel", "7"), value("D2", "doel", "5"),
                        value("V1", "doel", null)));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(SISTER_INITIALISATIONS)) {
            List<String> names = TmsDocuments.ruleNames(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(SISTER_MODEL, order), caseDocument,
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
        List<String> withSupertype = new ArrayList<>(SISTER_INITIALISATIONS);
        withSupertype.add(computingRule("standaarddoel van een vondst", "INITIALISATIE", "Vondst", "doel", number("3"), "null"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocuments.document(SISTER_MODEL, withSupertype), DroolsBackend.Branch.EXEC_MODEL));
        assertEquals("transpiler SKIP: regel: standaarddoel van drijfgoed — twee of meer initialisaties op één doel"
                + " ('doel' van Vondst: \"standaarddoel van drijfgoed\", \"standaarddoel van een vondst\"), voorwaardelijk of"
                + " niet: waar ze allebei gelden rangschikt niets ze en bepaalt het fixpunt de waarde niet"
                + " (RUNTIME-SEMANTICS ch. 5.4, WF-079)", e.getMessage());
    }

    // ---------------- a computing rule beside a conditional initialisation ----------------

    private static final List<String> BESIDE_INITIALISATION = List.of(BETWEEN_RULE,
            computingRule("doel volgens b", "GELIJKSTELLING", "Vondst", "doel", B,
                    comparison("greater", BETWEEN, number("10"))),
            computingRule("standaarddoel", "INITIALISATIE", "Vondst", "doel", number("3"), predicate("filled", C)));

    @Test void beside_a_conditional_initialisation_in_every_order_of_the_document() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("a", "20", "b", "7", "c", "1", "tussen", "3"), List.of(),
                                Map.of()),
                        instance("V2", "Vondst", Map.of("a", "20", "doel", "9"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of("a", "1", "c", "1"), List.of(), Map.of()),
                        instance("V4", "Vondst", Map.of("a", "1", "c", "1", "doel", "9"), List.of(), Map.of())),
                List.of(value("V1", "doel", "7"), value("V2", "doel", null), value("V3", "doel", "3"),
                        value("V4", "doel", "9")));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(BESIDE_INITIALISATION)) {
            Path ir = TmsDocuments.document(MODEL, order);
            List<String> names = TmsDocuments.ruleNames(order);
            assertNull(CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            for (String label : List.of("V5", "V6")) {
                Map<String, String> input = label.equals("V5") ? Map.of("a", "20", "c", "1")
                        : Map.of("a", "20", "c", "1", "doel", "9");
                assertEquals(conflict(names, "doel volgens b", "leeg", "standaarddoel", "3", "doel", label + ", een Vondst"),
                        fault(ir, caseOf(label, "Vondst", input)),
                        () -> label + " in volgorde " + names);
            }
            orders++;
        }
        assertEquals(6, orders);
    }

    /** The rule of the tussen, its two standing rules, the computing rule, the initialisation and the rule that
     *  compares the values: a sample of their orders over V1 to V4, written back without a case label, and over V5. */
    @Test void beside_a_conditional_initialisation_in_a_sample_of_generated_orders() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, BESIDE_INITIALISATION),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(6, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        for (List<Rule> order : TmsDocuments.sample(generated, 20261009L, 60)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = beanObject(b, Map.of("a", "20", "b", "7", "c", "1", "tussen", "3")),
                    v2 = beanObject(b, Map.of("a", "20", "doel", "9")), v3 = beanObject(b, Map.of("a", "1", "c", "1")),
                    v4 = beanObject(b, Map.of("a", "1", "c", "1", "doel", "9"));
            TmsDocuments.fire(b, order, List.of(v1, v2, v3, v4), s -> {}, s -> {});
            String where = " in volgorde " + names;
            assertNumber("7", v1, "doel", "V1" + where);
            assertNumber(null, v2, "doel", "V2" + where);
            assertNumber("3", v3, "doel", "V3" + where);
            assertNumber("9", v4, "doel", "V4" + where);
            Object v5 = beanObject(b, Map.of("a", "20", "c", "1"));
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> TmsDocuments.fire(b, order, List.of(v5), s -> {}, s -> {}), () -> "V5" + where);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"doel volgens"
                    + " b\" schrijft leeg en de regel \"standaarddoel\" schrijft 3 in het attribuut doel van een Vondst"
                    + " (Vondst@" + Integer.toHexString(System.identityHashCode(v5)) + "); divergentie is een fout"
                    + " (RUNTIME-SEMANTICS ch. 5.7)", e.getMessage(), "V5" + where);
        }
    }

    // ---------------- the red case of the corpus ----------------

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path DOUBLE_MESSAGE = Path.of("..", "..", "corpus", "red", "jutterswet-dubbele-melding");

    @Test void the_red_case_gives_the_fault_with_the_sentence_of_ref() throws Exception {
        List<Path> cases;
        try (var s = Files.list(DOUBLE_MESSAGE)) {
            cases = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(2, cases.size());
        for (Path z : cases) {
            JsonNode c = TmsDocuments.JSON.readTree(Files.readString(z));
            JsonNode pin = c.path("expectedFault");
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> CaseRunner.playCase(DOUBLE_MESSAGE.resolve("ir.json"), c, DroolsBackend.Branch.EXEC_MODEL));
            assertTrue(e.getMessage().contains("twee schrijvers van één doel"), () -> z + ": " + e.getMessage());
            // the pin is the fault kind and its parameters: the code and every pinned parameter are the backend's own
            CaseRunner.FaultKind kind = CaseRunner.faultKinds(e).stream()
                    .filter(k -> k.code().equals(pin.path("code").asText())).findFirst().orElse(null);
            assertTrue(kind != null, () -> z + ": " + pin.path("code").asText() + " is not among " + CaseRunner.faultKinds(e));
            pin.path("params").fields().forEachRemaining(p ->
                    assertEquals(p.getValue().asText(), kind.params().get(p.getKey()), () -> z + ": " + p.getKey()));
        }
    }

    // ---------------- helpers ----------------

    /** The sentence of RUNTIME-SEMANTICS ch. 5.7 for the two writers {@code a} and {@code b}, in the order of the
     *  document whose rules are {@code names}. */
    private static String conflict(List<String> names, String a, String valueA, String b, String valueB, String member,
                                   String object) {
        boolean aFirst = names.indexOf(a) < names.indexOf(b);
        String first = aFirst ? a : b, second = aFirst ? b : a;
        String firstValue = aFirst ? valueA : valueB, secondValue = aFirst ? valueB : valueA;
        return "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"" + first
                + "\" schrijft " + firstValue + " en de regel \"" + second + "\" schrijft " + secondValue
                + " in het attribuut " + member + " van " + object + "; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
    }

    /** The message of the fault the replay of {@code caseDocument} on this branch ends in. */
    private static String fault(Path ir, JsonNode caseDocument) {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
        return e.getMessage();
    }

    /** A case of one instance whose replay faults: its expectation is never read. */
    private static JsonNode caseOf(String label, String type, Map<String, String> input) throws Exception {
        return TmsDocuments.caseDocument(List.of(instance(label, type, input, List.of(), Map.of())),
                List.of(value(label, "doel", "0")));
    }

    private static Object beanObject(DroolsBackend.Build b, Map<String, String> numbers) throws Exception {
        return TmsDocuments.beanObject(b.beanLoader(), "Vondst", numbers, List.of());
    }

    private static void assertNumber(String expected, Object beanObject, String attribute, String description) throws Exception {
        Object actual = TmsDocuments.read(beanObject, attribute);
        if (expected == null) assertNull(actual, description + " " + attribute);
        else assertEquals(0, new java.math.BigDecimal(expected).compareTo((java.math.BigDecimal) actual),
                () -> description + " " + attribute + ": verwacht " + expected + ", werkelijk " + actual);
    }
}
