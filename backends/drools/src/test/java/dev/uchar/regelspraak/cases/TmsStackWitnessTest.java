package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import org.drools.model.Rule;
import org.kie.api.KieBase;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The effective stack of RUNTIME-SEMANTICS ch. 7.10 in the shape of {@code jutterswet-wrakinitialisatie}: an
 * initialisation "het bergloon van een Vondst = 2 maal zijn gewicht indien zijn gewicht groter is dan 10" on Vondst,
 * and on {@code Wrakvondst is een Vondst} a defaults group, 9 where the gewicht is above 50 and above 100, 5 where it is
 * above 50. On a Wrakvondst the initialisation is folded into the stack below the group, with its condition, and is
 * read on Vondst, the level that declared it; there it must not fire as a rule as well, while on a Vondst it fires as
 * the ordinary initialisation it is. Played through the replay, and fired in a seeded sample of orders of the
 * generated rules, in which the rule never fires on a Wrakvondst and the stack's initialisation default does fire on
 * the Wrakvondst it fills.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing a rule on a target a defaults group
 * writes): every test on the branch failed on the build, "the_folded_initialisation_fills_under_its_condition:103 »
 * Excluded tms-transpiler SKIP: regel: standaardbergloon — een doel dat ook een defaultsgroep schrijft ('bergloon' van
 * Vondst): de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model branch passed the same case.
 *
 * <p><b>Red with the rule left on the subtype</b> (measured 2026-10-08 on a mutant that dropped the exclusion of the
 * stack's type from the rule's pattern, not committed): the values held, since the initialisation's guard yields to
 * the stack's write, but the sample failed on "de initialisatie vuurde als regel op [W4, W3, W2] in volgorde
 * [standaardbergloon, defaults A:Wrakvondst.bergloon: default 0, defaults A:Wrakvondst.bergloon: default 1, defaults
 * A:Wrakvondst.bergloon: default 2] ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;".
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4): the replay runs on the
 * exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, and the sample of orders on both: on
 * {@code DRL_TEXT} the rules of the plan rendered as text in each order ({@code TextOrder}), since the order of the
 * rules in the text is the order in which Drools loads them. <b>Red</b> on {@code e2bd685e}, before the step: both rows
 * failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet", the replay through the case and the
 * sample where it builds the branch.
 */
class TmsStackWitnessTest {

    private static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Vondst", List.of(attribute("gewicht", WHOLE), attribute("bergloon", WHOLE)),
                    List.of()),
            TmsDocuments.subtype("Wrakvondst", "Vondst", List.of(), List.of())), List.of());

    private static final String INITIALISATION = computingRule("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon",
            binary("times", number("2"), ownRead("gewicht", "Vondst")),
            comparison("greater", ownRead("gewicht", "Vondst"), number("10")));

    private static final String GROUP = attributeGroup("wrakbergloon", "Wrakvondst", "bergloon", List.of(
            defaultEntry(compound("all", 0, List.of(
                            comparison("greater", ownRead("gewicht", "Wrakvondst"), number("50")),
                            comparison("greater", ownRead("gewicht", "Wrakvondst"), number("100")))),
                    number("9")),
            defaultEntry(comparison("greater", ownRead("gewicht", "Wrakvondst"), number("50")), number("5"))));

    /** Per object its type, gewicht, bergloon in the input (null for none), and bergloon at the end (null for leeg). */
    private record Case(String type, String weight, String input, String bergloon) {}

    private static final Map<String, Case> CASES = new LinkedHashMap<>();
    static {
        CASES.put("W1", new Case("Wrakvondst", "8", null, null));
        CASES.put("W2", new Case("Wrakvondst", "12", null, "24"));
        CASES.put("W3", new Case("Wrakvondst", "60", null, "5"));
        CASES.put("W4", new Case("Wrakvondst", "120", null, "9"));
        CASES.put("W5", new Case("Wrakvondst", "12", "40", "40"));
        CASES.put("V1", new Case("Vondst", "12", null, "24"));
        CASES.put("V2", new Case("Vondst", "8", null, null));
        CASES.put("V3", new Case("Vondst", "60", null, "120"));
        CASES.put("V4", new Case("Vondst", "12", "40", "40"));
    }

    private static Map<String, String> numbers(Case g) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("gewicht", g.weight());
        if (g.input() != null) out.put("bergloon", g.input());
        return out;
    }

    private static JsonNode caseDocument() throws Exception {
        List<String> instances = new ArrayList<>(), expectations = new ArrayList<>();
        CASES.forEach((label, g) -> {
            instances.add(instance(label, g.type(), numbers(g), List.of(), Map.of()));
            expectations.add(value(label, "bergloon", g.bergloon()));
        });
        expectations.add(consistent());
        return TmsDocuments.caseDocument(instances, expectations);
    }

    private static Path document() throws Exception {
        return TmsDocuments.document(MODEL, List.of(INITIALISATION), List.of(), List.of(GROUP));
    }

    @Test void the_folded_initialisation_fills_under_its_condition() throws Exception {
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
            assertNull(CaseRunner.playCase(document(), caseDocument(), branch), branch.toString());
    }

    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        int[] folded = {0};
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100))
            fireSample(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList(), folded);
        assertTrue(folded[0] > 0, "de initialisatiedefault van de stapel vuurde nooit op W2");
        DroolsBackend.Build t = DroolsBackend.build(document(), DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(generated.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        int[] inTheText = {0};
        for (List<String> order : TmsDocuments.sample(rules, 20261008L, 100))
            fireSample(t, TextOrder.kieBase(t, order), order, inTheText);
        assertTrue(inTheText[0] > 0, "de initialisatiedefault van de stapel vuurde in de tekst nooit op W2");
    }

    /** Fires the objects of {@link #CASES} through {@code kieBase}, the rules of {@code b} in the order {@code names}:
     *  the rule never fires on a Wrakvondst, and every bergloon ends as it should; counts in {@code folded} the
     *  firings of the stack's initialisation default on W2. */
    private static void fireSample(DroolsBackend.Build b, KieBase kieBase, List<String> names, int[] folded)
            throws Exception {
        Class<?> wrakvondst = Class.forName("dev.uchar.regelspraak.facts.Wrakvondst", true, b.beanLoader());
        {
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> beans = new ArrayList<>();
            for (Map.Entry<String, Case> e : CASES.entrySet()) {
                Object beanObject = TmsDocuments.beanObject(b.beanLoader(), e.getValue().type(), numbers(e.getValue()), List.of());
                labels.put(beanObject, e.getKey());
                beans.add(beanObject);
            }
            List<String> onTheSubtype = new ArrayList<>();
            TmsDocuments.fire(b, kieBase, names, beans, session -> session.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    String rule = e.getMatch().getRule().getName();
                    for (Object o : e.getMatch().getObjects()) {
                        if (!wrakvondst.isInstance(o)) continue;
                        if (rule.equals("standaardbergloon")) onTheSubtype.add(labels.get(o));
                        if (rule.endsWith("default 2") && "W2".equals(labels.get(o))) folded[0]++;
                    }
                }
            }), session -> { });
            assertTrue(onTheSubtype.isEmpty(), () -> "de initialisatie vuurde als regel op " + onTheSubtype + " op "
                    + b.branch() + " in volgorde " + names);
            for (Object beanObject : beans) {
                Case g = CASES.get(labels.get(beanObject));
                assertEquals(g.bergloon() == null ? null : new BigDecimal(g.bergloon()),
                        TmsDocuments.read(beanObject, "bergloon"), () -> labels.get(beanObject) + " bergloon op " + b.branch()
                                + " in volgorde " + names);
            }
        }
    }

    /** The fold is per concrete type: the Wrakvondst carries one stack, and the Vondst none. */
    @Test void a_stack_per_concrete_type() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<String> names = b.execModelTranspiler().defaultsGroups().stream().map(g -> g.name()).toList();
        assertEquals(List.of("defaults A:Wrakvondst.bergloon"), names);
        assertFalse(b.execModelTranspiler().defaultsGroups().get(0).general());
    }
}
