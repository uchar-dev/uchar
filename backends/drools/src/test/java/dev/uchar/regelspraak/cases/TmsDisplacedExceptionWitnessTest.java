package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import dev.uchar.regelspraak.transpiler.tms.Outcome;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.contextValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.groupDefault;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An exception group below a displaced default evaluates nothing on the exec-model branch: no value, no fault.
 * The reference interpreter reaches the exception groups of a default only where no higher default in its chain decides
 * ({@code decide} in {@code ref/lib/eval.ml}), at every depth (RUNTIME-SEMANTICS ch. 7.1, 7.9), and a form that does
 * not decide is not evaluated (PLAN §9, 2026-10-09). The rules of a member chain therefore wait on the positions above
 * every default whose exception group the chain is, not only on the positions above them in their own chain.
 *
 * <p>The documents: a Vondst, "Een Vondst is gestrand indien hij aangespoeld is", "Een Vondst is wrak indien hij
 * gestrand is", and a defaults group on the heffing whose default "10 indien hij wrak is" displaces a default carrying
 * a default that faults where it is evaluated, in five places: an exception group of the lower default; an exception
 * group of an exception group of it; an exception group of a default that a member chain displaces; and the first and
 * the second of two exception groups, the other one "30 indien hij geregistreerd is". The faulting default, in five
 * forms: "20 indien het Rekenjaar groter is dan 2025" and "het Rekenjaar indien hij geregistreerd is" in a session
 * without an evaluation date, an {@code Unresolved} value and an {@code Unresolved} justification, and "20 indien zijn
 * vinddatum plus 18446744073709551617 dagen later is dan 2026-01-01". V1 (aangespoeld, geregistreerd): heffing 10,
 * consistent; the displacing default reads wrak, which the two rules derive later. V2 (geregistreerd): nothing
 * displaces the lower default, and the session faults with the reference interpreter's sentence. Beside them the
 * document of the lens that found the defect: no rules, wrak as input, the first faulting form below the lower
 * default.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner on every document in both orders of its rules,
 * each document verifier-clean): V1 passes everywhere, heffing 10 and consistent; V2 faults with "the evaluationYear
 * context value needs an evaluation date; the case carries asOfDate null", "Unresolved reached in evaluation: this rule
 * fails observably", "Unresolved condition reached in evaluation" and "duration count 18446744073709551617 is out of
 * range". The ASP runner declines every one of these documents at translation (the context value without a date,
 * "Unresolved reached in translation", a count beyond its 32-bit integers). The deposit variant, a division by a leeg
 * noemer in the justification of the faulting default, which this branch refuses as a division in a default: the
 * reference interpreter passes V1 consistent, and the ASP runner deposits on V1, under a rule that is not the
 * default's ("wrakverklaring × V1" with the two rules, "? × V1" without them).
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on 8185e0d6): all three tests failed. Every V1, in every place,
 * form and order, ended in the fault of the displaced exception group, none of the fifty played; the lens document
 * ended in "Rekenfout: fout in de regel "defaults A:Vondst.heffing": the evaluationYear context value needs an
 * evaluation date; the case carries asOfDate null"; and the sample failed in its first order on that fault.
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, rest of step 4; acceptance (a), the
 * nested exception groups of PRIORITIZED-DEFAULTS-EVOLUTIE §5.2 under a displaced default): the document of the lens and
 * every place, form and order play on the exec-model branch and on {@code DRL_TEXT}, and the sample of orders runs on
 * both, on {@code DRL_TEXT} with the rules of the text in each order ({@code TextOrder}): there too a displaced
 * exception group is evaluated first in some order, and its fault goes again. <b>Red</b> on {@code e2bd685e}, before
 * step 4: the three tests failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet".
 */
class TmsDisplacedExceptionWitnessTest {

    private static final String WHOLE = numberType(true, 0, null), DAGEN = numberType(true, 0, unit("dagen"));
    private static final String TOO_LARGE = "18446744073709551617";
    private static final String STACK = "defaults A:Vondst.heffing";
    private static final String REKENJAAR = "the evaluationYear context value needs an evaluation date; the case"
            + " carries asOfDate null";

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("heffing", WHOLE), attribute("vinddatum", DATE)),
            List.of("wrak", "gestrand", "aangespoeld", "geregistreerd"))), List.of());

    private static final List<String> RULES = List.of(
            characteristicRule("stranding", "Vondst", "gestrand", check("aangespoeld", false)),
            characteristicRule("wrakverklaring", "Vondst", "wrak", check("gestrand", false)));

    private static final String TEN = defaultEntry(check("wrak", false), number("10"));
    private static final String THIRTY = defaultEntry(check("geregistreerd", false), number("30"));

    /** A default that faults where it is evaluated, and the sentence the reference interpreter faults with there. */
    private record Fault(String naam, String defaultEntry, String sentence) {}

    private static final List<Fault> FAULTS = List.of(
            new Fault("het rekenjaar in de rechtvaardiging", defaultEntry(comparison("greater",
                    contextValue("evaluationYear", WHOLE), literal("2025", WHOLE)), number("20")), REKENJAAR),
            new Fault("het rekenjaar als gevolg", defaultEntry(check("geregistreerd", false),
                    contextValue("evaluationYear", WHOLE)), REKENJAAR),
            new Fault("een onopgeloste waarde als gevolg", defaultEntry(check("geregistreerd", false),
                    "{\"node\":\"Unresolved\",\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"het niet-gemodelleerde"
                            + " tarief\"}}"), "Unresolved reached in evaluation: this rule fails observably"),
            new Fault("een onopgeloste rechtvaardiging", defaultEntry("{\"node\":\"Unresolved\",\"source\":{\"snippet\":"
                    + "\"een niet-gemodelleerde grond\"}}", number("20")), "Unresolved condition reached in evaluation"),
            new Fault("een datumstap buiten het bereik in de rechtvaardiging", defaultEntry(comparison("later",
                    binary("plus", ownRead("vinddatum", "Vondst"), literal(TOO_LARGE, DAGEN), DATE),
                    literal("2026-01-01", DATE)), number("20")), "duration count " + TOO_LARGE + " is out of range"));

    /** Where the faulting default sits: the defaults of the group, highest first, around it. */
    private record Place(String naam, Function<String, List<String>> defaults) {}

    private static final List<Place> PLACES = List.of(
            new Place("een uitzonderingsgroep van de lagere default",
                    f -> List.of(TEN, groupDefault(List.of(List.of(f))))),
            new Place("een uitzonderingsgroep van een uitzonderingsgroep van de lagere default",
                    f -> List.of(TEN, groupDefault(List.of(List.of(groupDefault(List.of(List.of(f)))))))),
            new Place("een uitzonderingsgroep van een default die een lidketen verdringt",
                    f -> List.of(groupDefault(List.of(List.of(TEN, groupDefault(List.of(List.of(f)))))))),
            new Place("het eerste van twee leden van de lagere default",
                    f -> List.of(TEN, groupDefault(List.of(List.of(f), List.of(THIRTY))))),
            new Place("het tweede van twee leden van de lagere default",
                    f -> List.of(TEN, groupDefault(List.of(List.of(THIRTY), List.of(f))))));

    private static String group(Place p, Fault f) {
        return attributeGroup("heffing van een vondst", "Vondst", "heffing", p.defaults().apply(f.defaultEntry()));
    }

    /** V1: the higher default decides 10 once wrak is derived. */
    private static JsonNode displaced() throws Exception {
        return TmsDocuments.caseOn(null, List.of(instanceWith("V1", "Vondst",
                        Map.of("vinddatum", dateValue("2026-03-01")), List.of("aangespoeld", "geregistreerd"))),
                List.of(value("V1", "heffing", "10"), consistent()));
    }

    /** V2: nothing displaces the lower default, so its exception groups are evaluated. */
    private static JsonNode reaches() throws Exception {
        return TmsDocuments.caseOn(null, List.of(instanceWith("V2", "Vondst",
                Map.of("vinddatum", dateValue("2026-03-01")), List.of("geregistreerd"))), List.of(consistent()));
    }

    private static String fault(Fault f) {
        return "ComputeFault: fout in de regel \"" + STACK + "\": " + f.sentence();
    }

    @Test void the_document_of_the_lens() throws Exception {
        String model = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
                List.of(attribute("heffing", WHOLE)), List.of("wrak"))), List.of());
        Path ir = TmsDocuments.document(model, List.of(), List.of(), List.of(group(PLACES.get(0), FAULTS.get(0))));
        JsonNode wrak = TmsDocuments.caseOn(null, List.of(instanceWith("V1", "Vondst", Map.of(), List.of("wrak"))),
                List.of(value("V1", "heffing", "10"), consistent()));
        JsonNode noWrak = TmsDocuments.caseOn(null, List.of(instanceWith("V2", "Vondst", Map.of(), List.of())),
                List.of(consistent()));
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
            assertEquals(List.of("gespeeld", fault(FAULTS.get(0))), List.of(TmsFaultWitnessTest.play(ir, wrak, branch),
                    TmsFaultWitnessTest.play(ir, noWrak, branch)), branch.toString());
    }

    @Test void every_place_and_every_fault_in_every_order_of_the_rules() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Place p : PLACES)
            for (Fault f : FAULTS)
                for (List<String> order : TmsDocuments.permutations(RULES)) {
                    Path ir = TmsDocuments.document(MODEL, order, List.of(), List.of(group(p, f)));
                    for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL,
                            DroolsBackend.Branch.DRL_TEXT)) {
                        String description = branch + ", " + p.naam() + ", " + f.naam() + ", " + TmsDocuments.ruleNames(order);
                        outcomes.add(description + ", V1: " + TmsFaultWitnessTest.play(ir, displaced(), branch));
                        expected.add(description + ", V1: gespeeld");
                        outcomes.add(description + ", V2: " + TmsFaultWitnessTest.play(ir, reaches(), branch));
                        expected.add(description + ", V2: " + fault(f));
                    }
                }
        assertEquals(expected, outcomes);
        assertEquals(PLACES.size() * FAULTS.size() * 2 * 2 * 2, outcomes.size());
    }

    /**
     * A seeded sample of the orders of the generated rules, on V1, with the fault in the justification and in the
     * consequence in every place: the fire reaches its fixpoint without throwing, no fault stands there, every
     * {@link Outcome} at the fixpoint decides 10, so no exception group below the displaced default settles anything,
     * and the heffing is 10. In some order a displaced exception group was evaluated before wrak was derived; its fault
     * went again when the higher default decided. The same on {@code DRL_TEXT}, with the rules of the text in a sample
     * of orders.
     */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        for (Place p : PLACES)
            for (Fault f : FAULTS.subList(0, 2)) {
                String description = p.naam() + ", " + f.naam();
                DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, RULES, List.of(),
                        List.of(group(p, f))), DroolsBackend.Branch.EXEC_MODEL);
                int intermediateState = 0;
                List<Rule> generated = b.execModelTranspiler().model().getRules();
                for (List<Rule> order : TmsDocuments.sample(generated, 20261009L, 40))
                    if (fireSample(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName)
                            .toList(), description)) intermediateState++;
                assertTrue(intermediateState > 0, description + ": geen volgorde rekende de verdrongen uitzonderingsgroep eerst uit");
                DroolsBackend.Build t = DroolsBackend.build(TmsDocuments.document(MODEL, RULES, List.of(),
                        List.of(group(p, f))), DroolsBackend.Branch.DRL_TEXT);
                List<String> rules = TextOrder.rules(t);
                assertEquals(generated.stream().map(Rule::getName).toList(), rules, description + ": de regels van de tekst");
                int inTheText = 0;
                for (List<String> order : TmsDocuments.sample(rules, 20261009L, 40))
                    if (fireSample(t, TextOrder.kieBase(t, order), order, description)) inTheText++;
                assertTrue(inTheText > 0, description + ": geen volgorde van de tekst rekende de verdrongen uitzonderingsgroep"
                        + " eerst uit");
            }
    }

    /** Fires V1 through {@code kieBase}, the rules of {@code b} in the order {@code names}: no fault stands at the
     *  fixpoint, every {@link Outcome} there decides 10, and the heffing is 10. Whether a fault stood on the way. */
    private static boolean fireSample(DroolsBackend.Build b, KieBase kieBase, List<String> names, String description)
            throws Exception {
        String where = description + " op " + b.branch() + ", volgorde " + names;
        Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of("aangespoeld", "geregistreerd"));
        TmsFaultWitnessTest.FaultFacts faults = new TmsFaultWitnessTest.FaultFacts();
        List<Object> outcomes = new ArrayList<>();
        fire(b, kieBase, v1, faults, outcomes, where);
        assertEquals(faults.inserted, faults.withdrawn, () -> where + ": een fout staat aan het fixpunt");
        for (Object u : outcomes)
            assertTrue(((Outcome) u).value() instanceof BigDecimal w && w.compareTo(BigDecimal.TEN) == 0,
                    () -> where + ": " + u + " staat aan het fixpunt");
        TmsFaultWitnessTest.assertNumber("10", TmsDocuments.read(v1, "heffing"), where + ": heffing");
        return faults.inserted > 0;
    }

    /** Fires {@code beanObject} through {@code kieBase}, which holds the rules of {@code b}: the fire must reach its fixpoint
     *  without throwing, the write-back must raise no fault, and {@code outcomes} receives the {@link Outcome} facts
     *  of the fixpoint. */
    private static void fire(DroolsBackend.Build b, KieBase kieBase, Object beanObject,
                             TmsFaultWitnessTest.FaultFacts faults, List<Object> outcomes, String description) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = kieBase.newKieSession();
        try {
            session.addEventListener(faults);
            session.insert(beanObject);
            DroolsBackend.Firing firing;
            try {
                firing = DroolsBackend.fire(b, session, 100_000);
            } catch (RuntimeException e) {
                fail(description + ": de fire wierp " + e);
                return;
            }
            assertTrue(firing.noFixpoint() == null, () -> description + ": geen fixpunt");
            try {
                DroolsBackend.setDerivedValues(b, session);
            } catch (RuntimeException e) {
                fail(description + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            outcomes.addAll(session.getObjects(o -> o instanceof Outcome));
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
