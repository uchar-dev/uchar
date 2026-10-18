package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.DATE_TIME;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.dateValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWith;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.valueJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The faults of date arithmetic on the exec-model branch (RUNTIME-SEMANTICS ch. 3.8): a duration count outside
 * [−2⁶², 2⁶² − 1] and a datum-tijd step that leaves the representable millisecond range are evaluation faults, which on
 * this branch are facts that count only where they stand at the fixpoint (PLAN §9, 2026-10-09), named as the reference
 * interpreter names them. Until this card they left the fire as an exception, so a count that lies outside the range
 * only on an intermediate state ended the session where the fixpoint has a value.
 *
 * <p>The transient document: a Vondst with a vinddatum, "Een Vondst is erkend indien hij gemeld is", a defaults group
 * on the wachtdagen, higher "op 3 indien hij erkend is" and lower "op 18446744073709551617 indien hij gemeld is", and
 * "De meldtermijn van een Vondst moet berekend worden als zijn vinddatum plus zijn wachtdagen". V1 (gemeld, vinddatum
 * 2026-03-01): wachtdagen 3, meldtermijn 2026-03-04, consistent. Where the lower default decides before the erkenning,
 * the meldtermijn steps 2⁶⁴ + 1 days, out of range; that fault goes again when the higher default decides.
 *
 * <p>The default document: the same group on the meldtermijn itself, higher "op zijn vinddatum plus 3 dagen indien hij
 * erkend is" and lower "op zijn vinddatum plus 18446744073709551617 dagen indien hij gemeld is": the fault arises in the
 * consequence of the lower default and goes when the higher one displaces it; and with the step in the justification
 * of the lower default, "op zijn vinddatum indien zijn vinddatum plus 18446744073709551617 dagen later is dan
 * 2026-01-01", the fault arises in its test and goes the same way.
 *
 * <p>The standing documents: the count and the step stand at the fixpoint, and the session reports them with the
 * sentence the reference interpreter gives, "duration count 18446744073709551617 is out of range" and "datum-tijd
 * arithmetic leaves the representable millisecond range".
 *
 * <p><b>Measured</b> (2026-10-09, on every order of every document): the reference interpreter's runner passes the
 * transient and the default documents, where it decides the defaults group after the erkenning, and faults on the
 * standing ones in the two sentences above. The ASP runner declines all of them at translation, its integers being 32
 * bits ("value 18446744073709551617 at 0 decimals exceeds clingo's integer range"). This branch, at the claim of the
 * card, threw both faults out of the fire, in every order of the transient document as well (below).
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): "[erkenning, meldtermijn]
 * ConsequenceException: Exception executing consequence for rule "meldtermijn" in dev.uchar.regelspraak.tms:
 * java.lang.IllegalStateException: duration count 18446744073709551617 is out of range", the same in the other order and
 * out of "defaults A:Vondst.meldtermijn: default 1" for the first default document, and for the standing ones that
 * exception instead of the fault fact, "ConsequenceException: … rule "sluittijdstip" …
 * java.lang.IllegalStateException: datum-tijd arithmetic leaves the representable millisecond range".
 */
class TmsDateFaultWitnessTest {

    private static final String WHOLE = numberType(true, 0, null), DAGEN = numberType(true, 0, unit("dagen"));

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst", List.of(
            attribute("vinddatum", DATE), attribute("wachtdagen", DAGEN), attribute("meldtermijn", DATE)),
            List.of("gemeld", "erkend"))), List.of());

    private static final String TOO_LARGE = "18446744073709551617";

    static final String ERKENNING = characteristicRule("erkenning", "Vondst", "erkend", check("gemeld", false));

    static final String MELDTERMIJN = computingRule("meldtermijn", "BEREKENING", "Vondst", "meldtermijn",
            binary("plus", ownRead("vinddatum", "Vondst"), ownRead("wachtdagen", "Vondst"), DATE), "null");

    static final String WACHTDAGEN = attributeGroup("wachtdagen van een vondst", "Vondst", "wachtdagen", List.of(
            defaultEntry(check("erkend", false), literal("3", WHOLE)),
            defaultEntry(check("gemeld", false), literal(TOO_LARGE, WHOLE))));

    static JsonNode caseDocument() throws Exception {
        return TmsDocuments.caseOn(null, List.of(instanceWith("V1", "Vondst",
                Map.of("vinddatum", dateValue("2026-03-01")), List.of("gemeld"))),
                List.of(valueJson("V1", "meldtermijn", dateValue("2026-03-04")), consistent()));
    }

    @Test void a_count_that_lies_outside_the_range_only_on_an_intermediate_state_does_not_count() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(ERKENNING, MELDTERMIJN))) {
            List<String> names = TmsDocuments.ruleNames(order);
            outcomes.add(names + " " + TmsFaultWitnessTest.play(TmsDocuments.document(MODEL, order, List.of(),
                    List.of(WACHTDAGEN)), caseDocument(), DroolsBackend.Branch.EXEC_MODEL));
            expected.add(names + " gespeeld");
        }
        assertEquals(expected, outcomes);
        assertIntermediateState(TmsDocuments.document(MODEL, List.of(ERKENNING, MELDTERMIJN), List.of(), List.of(WACHTDAGEN)));
    }

    /** The date steps of the lower default, in its consequence and in its justification. */
    static List<String> defaultGroups() {
        String vinddatum = ownRead("vinddatum", "Vondst");
        String three = literal("3", DAGEN), large = literal(TOO_LARGE, DAGEN);
        return List.of(
                attributeGroup("meldtermijn van een vondst", "Vondst", "meldtermijn", List.of(
                        defaultEntry(check("erkend", false), binary("plus", vinddatum, three, DATE)),
                        defaultEntry(check("gemeld", false), binary("plus", vinddatum, large, DATE)))),
                attributeGroup("meldtermijn van een vondst", "Vondst", "meldtermijn", List.of(
                        defaultEntry(check("erkend", false), binary("plus", vinddatum, three, DATE)),
                        defaultEntry(comparison("later", binary("plus", vinddatum, large, DATE),
                                literal("2026-01-01", DATE)), vinddatum))));
    }

    @Test void a_date_step_outside_the_range_in_a_displaced_default_does_not_count() throws Exception {
        for (String group : defaultGroups()) {
            Path ir = TmsDocuments.document(MODEL, List.of(ERKENNING), List.of(), List.of(group));
            assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, caseDocument(), DroolsBackend.Branch.EXEC_MODEL));
            assertIntermediateState(ir);
        }
    }

    /** Every order of the generated rules, on V1: the fire reaches its fixpoint without throwing, no fault stands
     *  there, the meldtermijn is 2026-03-04, and in at least one order the out-of-range step happened and its fault
     *  went again. */
    private static void assertIntermediateState(Path ir) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int intermediateState = 0, orders = 0;
        for (List<Rule> order : TmsDocuments.sample(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> names = order.stream().map(Rule::getName).toList();
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of("gemeld"));
            v1.getClass().getMethod(dev.uchar.regelspraak.transpiler.core.Namen.setter("vinddatum"),
                    java.time.LocalDate.class).invoke(v1, java.time.LocalDate.of(2026, 3, 1));
            TmsLetWitnessTest.Facts facts = new TmsLetWitnessTest.Facts();
            assertNull(TmsDocuments.fixpoint(b, order, List.of(v1), session -> session.addEventListener(facts)),
                    () -> "volgorde " + names);
            assertEquals(facts.inserted, facts.withdrawn, () -> "foutfeiten in volgorde " + names);
            assertEquals(java.time.LocalDate.of(2026, 3, 4), TmsDocuments.read(v1, "meldtermijn"),
                    () -> "meldtermijn in volgorde " + names);
            if (facts.faults > 0) intermediateState++;
            orders++;
        }
        assertTrue(intermediateState > 0, "geen volgorde liet de lagere default eerst beslissen");
        assertTrue(orders > 2);
    }

    private static final String TIME_MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Getijpost", List.of(
            attribute("meldtijdstip", DATE_TIME), attribute("wachttijd", numberType(true, 0, unit("uur"))),
            attribute("sluittijdstip", DATE_TIME), attribute("vinddatum", DATE), attribute("wachtdagen", DAGEN),
            attribute("meldtermijn", DATE)), List.of())), List.of());

    @Test void a_standing_date_fault_gives_the_sentence_of_the_reference() throws Exception {
        String close = computingRule("sluittijdstip", "BEREKENING", "Getijpost", "sluittijdstip",
                binary("plus", ownRead("meldtijdstip", "Getijpost"), ownRead("wachttijd", "Getijpost"), DATE_TIME), "null");
        String report = computingRule("meldtermijn", "BEREKENING", "Getijpost", "meldtermijn",
                binary("plus", ownRead("vinddatum", "Getijpost"), ownRead("wachtdagen", "Getijpost"), DATE), "null");
        JsonNode hours = TmsDocuments.caseOn(null, List.of(instanceWith("G1", "Getijpost", Map.of(
                "meldtijdstip", "{\"dateTime\":\"2024-01-01T00:00:00\"}", "wachttijd", numberValue("2000000000000")),
                List.of())), List.of(consistent()));
        JsonNode days = TmsDocuments.caseOn(null, List.of(instanceWith("G1", "Getijpost", Map.of(
                "vinddatum", dateValue("2026-03-01"), "wachtdagen", numberValue(TOO_LARGE)), List.of())),
                List.of(consistent()));
        assertEquals(List.of("ComputeFault: fout in de regel \"sluittijdstip\": datum-tijd arithmetic leaves the"
                        + " representable millisecond range",
                        "ComputeFault: fout in de regel \"meldtermijn\": duration count " + TOO_LARGE + " is out of range"),
                List.of(TmsFaultWitnessTest.play(TmsDocuments.document(TIME_MODEL, List.of(close)), hours,
                                DroolsBackend.Branch.EXEC_MODEL),
                        TmsFaultWitnessTest.play(TmsDocuments.document(TIME_MODEL, List.of(report)), days,
                                DroolsBackend.Branch.EXEC_MODEL)));
    }
}
