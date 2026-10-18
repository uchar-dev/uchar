package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.Decides;
import dev.uchar.regelspraak.transpiler.tms.ControlFact;
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
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fall-through at an empty default where the higher default decides after all (acceptance (b) and A10 of the trial of
 * card drools-logische-afleiding): two defaults on the {@code doel} of a Vondst. The higher one, justified where the
 * Vondst is {@code c} and not {@code afgewezen}, takes {@code bron1}, which a chain of two rules fills only where the
 * Vondst is {@code b}; the lower one, justified where it is {@code d}, takes {@code bron2}, which one rule fills.
 * RUNTIME-SEMANTICS ch. 7.1: the highest default that decides supplies the value, and a justified default whose
 * consequence is leeg falls through. Truth maintenance has to get there from either side: where the lower default
 * decides before {@code bron1} arrives, its write must go when the higher one decides (the takeover); where the higher
 * one decides before {@code afgewezen} is derived, its decision must go again and the lower one decide (the give-back).
 * Every combination of the four input characteristics is an object of one case, played in every order of the document's
 * rules, and fired in a seeded sample of orders of the rules the branch generates, which must take both transient paths
 * somewhere.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "every_order_of_the_rules_in_the_document:108 » Excluded tms-transpiler SKIP: defaults:
 * doel van een vondst — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model
 * branch passed the same case.
 *
 * <p><b>Red with a control fact or a write stated</b> (measured 2026-10-08 on two mutants of the simple form, not
 * committed; the document orders passed both, the sample did not). With {@code Beslist} inserted by {@code insert}
 * instead of {@code logicalInsert}, the give-back failed: "V1111 doel in volgorde [kenmerkstand Vondst.tussen: waar uit
 * een afleiding, …, defaults A:Vondst.doel: default 0, …, defaults A:Vondst.doel: default 1, tussenstap, afwijzing, …]
 * ==&gt; expected: &lt;200&gt; but was: &lt;null&gt;": the stated fact outlived the decision it recorded, and the lower
 * default stayed blocked after the higher one's justification went. With the {@code Waarde} of a decision stated, the
 * takeover failed: "IllegalState geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel
 * "defaults A:Vondst.doel" schrijft 100 en de regel "defaults A:Vondst.doel" schrijft 200 in het attribuut doel van een
 * Vondst".
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4; acceptance (a), the ordered
 * defaults with fall-through of PRIORITIZED-DEFAULTS-EVOLUTIE §5.2): every order of the document's rules plays on the
 * exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, and the sample of orders runs on both: on
 * {@code DRL_TEXT} the rules of the plan rendered as text in each order ({@code TextOrder}), since the order of the
 * rules in the text is the order in which Drools loads them, and the takeover and the give-back must each happen there
 * too. <b>Red</b> on {@code e2bd685e}, before the step: both rows failed on {@code DRL_TMS} with "drl-tms: de
 * DRL-weergave draagt Afloop nog niet".
 */
class TmsFallThroughWitnessTest {

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Vondst",
            List.of(attribute("bron1", WHOLE), attribute("bron2", WHOLE), attribute("doel", WHOLE)),
            List.of("b", "c", "d", "f", "tussen", "afgewezen"))), List.of());

    private static final List<String> RULES = List.of(
            characteristicRule("tussenstap", "Vondst", "tussen", check("b", false)),
            computingRule("eerste bron", "GELIJKSTELLING", "Vondst", "bron1", number("100"), check("tussen", false)),
            computingRule("tweede bron", "GELIJKSTELLING", "Vondst", "bron2", number("200"), "null"),
            characteristicRule("afwijzing", "Vondst", "afgewezen", check("f", false)));

    private static final String GROUP = attributeGroup("doel van een vondst", "Vondst", "doel", List.of(
            defaultEntry(compound("all", 0, List.of(check("c", false), check("afgewezen", true))),
                    ownRead("bron1", "Vondst")),
            defaultEntry(check("d", false), ownRead("bron2", "Vondst"))));

    /** The input characteristics of object {@code bits}: b, c, d and f by its four bits. */
    private static List<String> kenmerken(int bits) {
        List<String> k = new ArrayList<>();
        String[] names = {"b", "c", "d", "f"};
        for (int i = 0; i < 4; i++) if ((bits & (1 << i)) != 0) k.add(names[i]);
        return k;
    }

    /** The label of object {@code bits}, its four bits as b, c, d, f. */
    private static String label(int bits) {
        StringBuilder s = new StringBuilder("V");
        for (int i = 0; i < 4; i++) s.append((bits & (1 << i)) != 0 ? '1' : '0');
        return s.toString();
    }

    /** RUNTIME-SEMANTICS ch. 7.1 by hand: the higher default where it is justified and bron1 is filled, else the lower
     *  where it is justified (bron2 is always filled), else leeg. */
    private static String expected(int bits) {
        boolean b = (bits & 1) != 0, c = (bits & 2) != 0, d = (bits & 4) != 0, f = (bits & 8) != 0;
        return c && !f && b ? "100" : d ? "200" : null;
    }

    private static JsonNode caseDocument() throws Exception {
        List<String> instances = new ArrayList<>(), expectations = new ArrayList<>();
        for (int bits = 0; bits < 16; bits++) {
            instances.add(instance(label(bits), "Vondst", Map.of(), kenmerken(bits), Map.of()));
            expectations.add(value(label(bits), "doel", expected(bits)));
        }
        expectations.add(consistent());
        return TmsDocuments.caseDocument(instances, expectations);
    }

    private static Path document(List<String> rules) throws Exception {
        return TmsDocuments.document(MODEL, rules, List.of(), List.of(GROUP));
    }

    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = TmsDocuments.ruleNames(order);
            for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
                assertNull(CaseRunner.playCase(document(order), caseDocument(), branch), () -> branch + ": volgorde " + names);
                orders++;
            }
        }
        assertEquals(48, orders);
    }

    /** The two defaults, the four rules and the standings of tussen, afgewezen, bron1 and bron2: too many for every
     *  order, so a seeded sample, which must take the takeover and the give-back each at least once. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(RULES), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(16, generated.size(),
                () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        int[] adoption = {0}, returns = {0};
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100))
            fireSample(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList(), adoption,
                    returns);
        assertTrue(adoption[0] > 0, "geen volgorde liet de lagere default eerst beslissen en de hogere overnemen");
        assertTrue(returns[0] > 0, "geen volgorde liet de hogere default beslissen en zijn besluit weer verliezen");
        DroolsBackend.Build t = DroolsBackend.build(document(RULES), DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(generated.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        int[] textAdoption = {0}, textReturn = {0};
        for (List<String> order : TmsDocuments.sample(rules, 20261008L, 100))
            fireSample(t, TextOrder.kieBase(t, order), order, textAdoption, textReturn);
        assertTrue(textAdoption[0] > 0, "geen volgorde van de tekst liet de hogere default overnemen");
        assertTrue(textReturn[0] > 0, "geen volgorde van de tekst liet de hogere default zijn besluit verliezen");
    }

    /** Fires the sixteen Vondsten through {@code kieBase}, the rules of {@code b} in the order {@code namen}: every doel
     *  ends as ch. 7.1 says, and a simple group inserts no other control fact than {@code Beslist}; counts the takeovers
     *  and the give-backs. */
    private static void fireSample(DroolsBackend.Build b, KieBase kieBase, List<String> names, int[] adoption,
                                       int[] returns) throws Exception {
        {
            List<Object> beans = new ArrayList<>();
            for (int bits = 0; bits < 16; bits++)
                beans.add(TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), kenmerken(bits)));
            Map<Object, List<String>> fired = new IdentityHashMap<>();
            TmsDocuments.fire(b, kieBase, names, beans, session -> session.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    String regel = e.getMatch().getRule().getName();
                    for (Object o : e.getMatch().getObjects())
                        if (beans.stream().anyMatch(x -> x == o))
                            fired.computeIfAbsent(o, x -> new ArrayList<>()).add(regel);
                }
            }), session -> {
                for (Object f : session.getObjects(o -> o instanceof ControlFact))
                    assertTrue(f instanceof Decides, () -> "een eenvoudige groep zet alleen Beslist in: " + f);
            });
            for (int bits = 0; bits < 16; bits++) {
                Object beanObject = beans.get(bits);
                String where = label(bits) + " doel op " + b.branch() + " in volgorde " + names;
                String v = expected(bits);
                assertEquals(v == null ? null : new BigDecimal(v), TmsDocuments.read(beanObject, "doel"), where);
                List<String> rules = fired.getOrDefault(beanObject, List.of());
                if ("100".equals(v) && rules.stream().anyMatch(r -> r.endsWith("default 1"))) adoption[0]++;
                if ("200".equals(v) && rules.stream().anyMatch(r -> r.endsWith("default 0"))) returns[0]++;
            }
        }
    }
}
