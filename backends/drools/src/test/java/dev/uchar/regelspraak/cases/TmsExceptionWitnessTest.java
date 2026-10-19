package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.ControlFact;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import dev.uchar.regelspraak.transpiler.tms.Outcome;
import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

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
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.groupDefault;
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.withExceptions;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nested exception groups (RUNTIME-SEMANTICS ch. 7.9) through the branch's control facts, in the shapes of the anchors
 * {@code jutterswet-nestuitzondering} and {@code jutterswet-nestdiepte} on one target: default 0 decides 40 where the
 * Jutter is {@code grondX}, with three unordered exception groups, 10 on {@code grondY}, 10 on {@code grondW}, and 30 on
 * {@code grondP}, which itself carries the unordered groups 5 on {@code grondQ} and 25 on {@code grondR}; default 1 is a
 * group-default over 15 on {@code grondB1} and 25 on {@code grondB2}. {@code grondY} and {@code grondQ} are derived from
 * inputs by a rule each, so a reconciliation can see a member decide before another one does.
 *
 * <p>What the objects pin, each against the conflict facts exactly, not only the ones expected: agreeing members (10
 * and 10) are one decision; a deeper conflict ({@code Jutter.heffing/0/0}) passes up unchanged, also over a member that
 * decides; differing members conflict at their own path ({@code Jutter.heffing/0}); a form-a default falls back to its
 * own value where no member settles; and a higher default that settles displaces the exception groups of a lower one,
 * conflict included (no {@code Jutter.heffing/1} fact beside 40). Played in every order of the document's rules and
 * fired in a seeded sample of orders of the generated rules, where truth maintenance must withdraw a control fact
 * somewhere on the way.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "every_order_of_the_rules_in_the_document:113 » Excluded tms-transpiler SKIP: defaults:
 * heffing van een jutter — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet"; the exec-model
 * branch passed the same case.
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, rest of step 4; acceptance (a), the
 * unordered defaults with §9.5 and the nested exception groups of PRIORITIZED-DEFAULTS-EVOLUTIE §5.2): every order of
 * the document's rules plays on the exec-model branch and on {@code DRL_TEXT}, and the sample of orders runs on both,
 * on {@code DRL_TEXT} with the rules of the text in each order ({@code TextOrder}); a control fact must be
 * withdrawn on the way there too. <b>Red</b> on {@code e2bd685e}, before step 4: both tests failed on {@code DRL_TMS}
 * with "drl-tms: de DRL-weergave draagt Afloop nog niet".
 */
class TmsExceptionWitnessTest {

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objectType("Jutter",
            List.of(attribute("heffing", WHOLE)),
            List.of("grondX", "grondY", "grondW", "grondP", "grondQ", "grondR", "grondB1", "grondB2", "y0", "q0"))),
            List.of());

    private static final List<String> RULES = List.of(
            characteristicRule("grond y", "Jutter", "grondY", check("y0", false)),
            characteristicRule("grond q", "Jutter", "grondQ", check("q0", false)));

    private static final String GROUP = attributeGroup("heffing van een jutter", "Jutter", "heffing", List.of(
            withExceptions(check("grondX", false), number("40"), List.of(
                    List.of(defaultEntry(check("grondY", false), number("10"))),
                    List.of(defaultEntry(check("grondW", false), number("10"))),
                    List.of(withExceptions(check("grondP", false), number("30"), List.of(
                            List.of(defaultEntry(check("grondQ", false), number("5"))),
                            List.of(defaultEntry(check("grondR", false), number("25")))))))),
            groupDefault(List.of(
                    List.of(defaultEntry(check("grondB1", false), number("15"))),
                    List.of(defaultEntry(check("grondB2", false), number("25")))))));

    /** Per object its input characteristics, its heffing (null for leeg) and its conflict key (null for none). */
    private record Case(List<String> kenmerken, String heffing, String conflict) {}

    private static final Map<String, Case> CASES = new LinkedHashMap<>();
    static {
        CASES.put("J1", new Case(List.of("grondX", "y0"), "10", null));
        CASES.put("J2", new Case(List.of("grondX", "y0", "grondW"), "10", null));
        CASES.put("J3", new Case(List.of("grondX", "y0", "grondP"), null, "Jutter.heffing/0"));
        CASES.put("J4", new Case(List.of("grondX", "grondP", "q0"), "5", null));
        CASES.put("J5", new Case(List.of("grondX", "grondP", "q0", "grondR"), null, "Jutter.heffing/0/0"));
        CASES.put("J6", new Case(List.of("grondX"), "40", null));
        CASES.put("J7", new Case(List.of("grondX", "grondB1", "grondB2"), "40", null));
        CASES.put("J8", new Case(List.of("grondB1", "grondB2"), null, "Jutter.heffing/1"));
        CASES.put("J9", new Case(List.of("grondB1"), "15", null));
        CASES.put("J10", new Case(List.of(), null, null));
        CASES.put("J11", new Case(List.of("grondX", "y0", "grondP", "q0"), null, "Jutter.heffing/0"));
        CASES.put("J12", new Case(List.of("grondX", "y0", "grondP", "q0", "grondR"), null, "Jutter.heffing/0/0"));
    }

    private static JsonNode caseDocument() throws Exception {
        List<String> instances = new ArrayList<>(), expectations = new ArrayList<>();
        CASES.forEach((label, g) -> {
            instances.add(instance(label, "Jutter", Map.of(), g.kenmerken(), Map.of()));
            expectations.add(value(label, "heffing", g.heffing()));
            if (g.conflict() != null) expectations.add(inconsistent(g.conflict(), label));
        });
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
        assertEquals(4, orders);
    }

    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(RULES), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        int[] withdrawn = {0};
        for (List<Rule> order : TmsDocuments.sample(generated, 20261008L, 100))
            fireSample(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList(),
                    withdrawn);
        assertTrue(withdrawn[0] > 0, "geen volgorde trok een stuurfeit in: de steekproef liep geen tussenstand langs");
        DroolsBackend.Build t = DroolsBackend.build(document(RULES), DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(generated.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        int[] inTheText = {0};
        for (List<String> order : TmsDocuments.sample(rules, 20261008L, 100))
            fireSample(t, TextOrder.kieBase(t, order), order, inTheText);
        assertTrue(inTheText[0] > 0, "geen volgorde van de tekst trok een stuurfeit in");
    }

    /** Fires the Jutters of {@link #CASES} through {@code kieBase}, the rules of {@code b} in the order {@code names}:
     *  the conflicts and the heffing of each are as pinned, and a general group inserts no other control fact than
     *  {@code Outcome}; counts in {@code withdrawn} the outcomes truth maintenance withdrew. */
    private static void fireSample(DroolsBackend.Build b, KieBase kieBase, List<String> names, int[] withdrawn)
            throws Exception {
        Map<Object, String> labels = new IdentityHashMap<>();
        List<Object> beans = new ArrayList<>();
        for (Map.Entry<String, Case> e : CASES.entrySet()) {
            Object beanObject = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), e.getValue().kenmerken());
            labels.put(beanObject, e.getKey());
            beans.add(beanObject);
        }
        List<String> conflicts = new ArrayList<>();
        TmsDocuments.fire(b, kieBase, names, beans, session -> session.addEventListener(new RuleRuntimeEventListener() {
            @Override public void objectInserted(ObjectInsertedEvent e) { }
            @Override public void objectUpdated(ObjectUpdatedEvent e) { }
            @Override public void objectDeleted(ObjectDeletedEvent e) {
                if (e.getOldObject() instanceof Outcome) withdrawn[0]++;
            }
        }), session -> {
            conflicts.addAll(TmsDocuments.inconsistencies(session, labels));
            for (Object f : session.getObjects(o -> o instanceof ControlFact))
                assertTrue(f instanceof Outcome, () -> "een algemene groep zet alleen Uitkomst in: " + f);
        });
        List<String> expected = new ArrayList<>();
        CASES.forEach((label, g) -> { if (g.conflict() != null) expected.add(g.conflict() + " @ " + label); });
        expected.sort(null);
        assertEquals(expected, conflicts, () -> "conflicten op " + b.branch() + " in volgorde " + names);
        for (Object beanObject : beans) {
            Case g = CASES.get(labels.get(beanObject));
            assertEquals(g.heffing() == null ? null : new BigDecimal(g.heffing()), TmsDocuments.read(beanObject, "heffing"),
                    () -> labels.get(beanObject) + " heffing op " + b.branch() + " in volgorde " + names);
        }
    }
}
