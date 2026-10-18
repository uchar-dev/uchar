package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.State;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
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
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Case input on an attribute that a rule computes, on the exec-model branch: "Het bergloon van een Vondst moet
 * gesteld worden op zijn taxatie" and "De uitkering van een Vondst moet berekend worden als zijn bergloon plus 5". The
 * rule is the bergloon's only writer, so the standing norm of RUNTIME-SEMANTICS ch. 7.8 holds: where the rule decides,
 * its value replaces the input; where it computes leeg, it writes nothing and the input stands. V1 has taxatie 30 and
 * bergloon 7 as input: bergloon 30, uitkering 35. V2 has no taxatie and bergloon 7: bergloon 7, uitkering 12. V3 has
 * neither: bergloon leeg, uitkering 5. The uitkering reads the bergloon through its standing, which carries the
 * derived value where there is one and the input otherwise; when the derived value arrives after the uitkering was
 * computed from the input, truth maintenance retracts the input standing and the uitkering computed from it. Both
 * orders of the two rules in the document, and a seeded sample of orders of the generated rules, at least one of which
 * must compute V1's uitkering from the input first. Off the standing domain the rule writes a computed leeg (ch. 1.2g),
 * which empties the target, input included.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step): both rows on this branch were
 * refused, "tms-transpiler SKIP: regel: bergloon — een attribuutdoel (gelijkstelling van 'bergloon' van Vondst): de
 * truth-maintenance-tak leidt nog alleen kenmerken af". The row on the exec-model branch was green from the start.
 * Without the refusal off the standing domain (measured the same day, on the lowering), the refusal row got the
 * sentence of the initialisation instead: "but was: &lt;tms-transpiler SKIP: regel: standaardbergloon — een
 * initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet&gt;".
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the input standing derived without its
 * {@code not Waarde}, V1 kept the input standing beside the derived one, the uitkering was computed from both, and the
 * write-back failed both rows: "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel
 * "uitkering" schrijft 12 en de regel "uitkering" schrijft 35 in het attribuut uitkering van een Vondst (Vondst@…);
 * divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)".
 */
class TmsInputOnComputedTargetWitnessTest {

    private static final List<String> RULES = List.of(
            computingRule("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", ownRead("taxatie", "Vondst"), "null"),
            computingRule("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binary("plus", ownRead("bergloon", "Vondst"), number("5")), "null"));

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("taxatie", WHOLE), attribute("bergloon", WHOLE), attribute("uitkering", WHOLE)),
            List.of())), List.of());

    @Test void both_orders_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(2, orders);
    }

    /** "Het bergloon van een Vondst moet geïnitialiseerd worden op 0, indien zijn taxatie gevuld is" beside the bergloon
     *  rule: a conditional initialisation is neither the plain one of RUNTIME-SEMANTICS ch. 7.7 nor absent, so the
     *  target is off the standing domain, where a computed leeg empties the target instead of leaving the input (ch.
     *  1.2g). Until step 3 of card tms-expressievormen-en-rekendatum the branch refused the rule there, since it wrote
     *  nothing for a computed leeg; it now writes the leeg ({@code TmsWritersWitnessTest}). V1 (taxatie 30, bergloon
     *  7 as input): 30, uitkering 35. V2 (bergloon 7 as input): the rule writes leeg over the input and the
     *  initialisation's condition does not hold, so bergloon leeg and uitkering 5. Every order of the three rules. The
     *  reference interpreter answers the same on this document (measured 2026-10-09, in the report of that step). */
    @Test void outside_the_standing_domain_writes_the_computing_rule_empty() throws Exception {
        String initialisatie = computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", number("0"),
                predicate("filled", ownRead("taxatie", "Vondst")));
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("bergloon", "7"), List.of(), Map.of())),
                List.of(value("V1", "bergloon", "30"), value("V1", "uitkering", "35"), value("V2", "bergloon", null),
                        value("V2", "uitkering", "5")));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(List.of(RULES.get(0), RULES.get(1), initialisatie))) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument,
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    /** Two rules and the two standing rules of bergloon; taxatie is input and read off the bean. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, RULES),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(4, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        int outOfTheInput = 0;
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 40))
            if (fire(b, order)) outOfTheInput++;
        assertTrue(outOfTheInput > 0, "geen volgorde berekende de uitkering van V1 uit de invoer");
    }

    /** Fires V1, V2 and V3 with the rules in {@code order}, writes back, checks the beans, and says whether the
     *  uitkering rule fired on V1 over the input standing of its bergloon, which the derived value replaces. */
    private static boolean fire(DroolsBackend.Build b, List<Rule> order) throws Exception {
        List<String> names = order.stream().map(Rule::getName).toList();
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession session = TmsDocuments.kieBase(b, order).newKieSession();
        try {
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of());
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("bergloon", "7"), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of());
            boolean[] outOfTheInput = {false};
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("uitkering")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof State s && s.onderwerp() == v1
                                    && new BigDecimal("7").equals(s.value())))
                        outOfTheInput[0] = true;
                }
            });
            session.insert(v1);
            session.insert(v2);
            session.insert(v3);
            assertNull(DroolsBackend.fire(b, session, 100_000).noFixpoint(), () -> "geen fixpunt in volgorde " + names);
            DroolsBackend.setDerivedValues(b, session);
            String where = " in volgorde " + names;
            assertNumber("30", TmsDocuments.read(v1, "bergloon"), "V1 bergloon" + where);
            assertNumber("35", TmsDocuments.read(v1, "uitkering"), "V1 uitkering" + where);
            assertNumber("7", TmsDocuments.read(v2, "bergloon"), "V2 bergloon" + where);
            assertNumber("12", TmsDocuments.read(v2, "uitkering"), "V2 uitkering" + where);
            assertNull(TmsDocuments.read(v3, "bergloon"), "V3 bergloon" + where);
            assertNumber("5", TmsDocuments.read(v3, "uitkering"), "V3 uitkering" + where);
            return outOfTheInput[0];
        } finally {
            session.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static void assertNumber(String expected, Object actual, String description) {
        assertTrue(actual instanceof BigDecimal w && new BigDecimal(expected).compareTo(w) == 0,
                () -> description + ": verwacht " + expected + ", werkelijk " + actual);
    }

    private static JsonNode caseDocument() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("bergloon", "7"), List.of(), Map.of()),
                        instance("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(value("V1", "bergloon", "30"), value("V1", "uitkering", "35"),
                        value("V2", "bergloon", "7"), value("V2", "uitkering", "12"),
                        value("V3", "bergloon", null), value("V3", "uitkering", "5")));
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
