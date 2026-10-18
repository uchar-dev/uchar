package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import org.drools.model.Rule;
import org.kie.api.KieBase;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.decision;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A defaults group on a characteristic (RUNTIME-SEMANTICS ch. 7.1 and 7.8): {@code vrijgesteld} of a Jutter, decided
 * do-not-attribute where it is {@code ny} and attribute where it is {@code nx}, in that order. A justified
 * characteristic default always decides, so the do-not-attribute decision displaces the attribution below it and the
 * group derives nothing; that is not a retraction, so a {@code vrijgesteld} the case input sets stays set. {@code ny}
 * is derived from an input by a rule, so the lower default can attribute before the higher one decides, and that
 * attribution has to go again. Played through the replay, and fired in a seeded sample of orders of the generated
 * rules, which must take that path somewhere.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08, the branch refusing every defaults group): the replay and the
 * sample failed on the build, "the_decision_false_displaces_and_the_input_stays_standing:79 » Excluded tms-transpiler SKIP:
 * defaults: vrijstelling van een jutter — een PrioritizedDefaults: de truth-maintenance-tak draagt deze vorm nog niet";
 * the exec-model branch passed the same case.
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, step 4; acceptance (a), the
 * characteristic defaults of PRIORITIZED-DEFAULTS-EVOLUTIE §5.2): the replay runs on the exec-model branch and on
 * {@code DRL_TEXT}, and the sample of orders on both, on {@code DRL_TEXT} with the rules of the text in each order
 * ({@code TextOrder}); the displacement must happen in the text too. <b>Red</b> on {@code e2bd685e}, before the
 * step: both rows failed on {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet".
 */
class TmsCharacteristicDecisionWitnessTest {

    private static final String MODEL = TmsDocuments.model(List.of("nx", "ny", "y0", "vrijgesteld"));

    private static final List<String> RULES = List.of(characteristicRule("grond ny", "ny", check("y0", false)));

    private static final String GROUP = characteristicGroup("vrijstelling van een jutter", "Jutter", "vrijgesteld", List.of(
            decision(check("ny", false), false),
            decision(check("nx", false), true)));

    /** Per object its input characteristics and whether it ends {@code vrijgesteld}. */
    private static final Map<String, List<String>> INVOER = new LinkedHashMap<>();
    private static final Map<String, Boolean> VRIJGESTELD = new LinkedHashMap<>();
    static {
        caseOf("J1", List.of("nx"), true);
        caseOf("J2", List.of("nx", "y0"), false);
        caseOf("J3", List.of("y0", "vrijgesteld"), true);
        caseOf("J4", List.of("nx", "y0", "vrijgesteld"), true);
        caseOf("J5", List.of(), false);
        caseOf("J6", List.of("nx", "vrijgesteld"), true);
    }

    private static void caseOf(String label, List<String> kenmerken, boolean vrijgesteld) {
        INVOER.put(label, kenmerken);
        VRIJGESTELD.put(label, vrijgesteld);
    }

    private static JsonNode caseDocument() throws Exception {
        List<String> instanties = new ArrayList<>(), verwachtingen = new ArrayList<>();
        INVOER.forEach((label, k) -> {
            instanties.add(TmsDocuments.jutter(label, k));
            verwachtingen.add(verwacht(label, "vrijgesteld", VRIJGESTELD.get(label)));
        });
        verwachtingen.add(consistent());
        return TmsDocuments.caseDocument(instanties, verwachtingen);
    }

    private static Path document() throws Exception {
        return TmsDocuments.document(MODEL, RULES, List.of(), List.of(GROUP));
    }

    @Test void the_decision_false_displaces_and_the_input_stays_standing() throws Exception {
        for (DroolsBackend.Branch branch : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT))
            assertNull(CaseRunner.playCase(document(), caseDocument(), branch), branch.toString());
    }

    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int[] displaced = {0};
        for (List<Rule> order : TmsDocuments.sample(gegenereerd, 20261008L, 100))
            fireSample(b, TmsDocuments.kieBase(b, order), order.stream().map(Rule::getName).toList(), displaced);
        assertTrue(displaced[0] > 0,
                "geen volgorde liet de toekenning eerst vuren en het besluit onwaar haar verdringen");
        DroolsBackend.Build t = DroolsBackend.build(document(), DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        int[] inTheText = {0};
        for (List<String> order : TmsDocuments.sample(rules, 20261008L, 100))
            fireSample(t, TextOrder.kieBase(t, order), order, inTheText);
        assertTrue(inTheText[0] > 0, "geen volgorde van de tekst liet het besluit onwaar de toekenning verdringen");
    }

    /** Fires the Jutters of {@code INVOER} through {@code kieBase}, the rules of {@code b} in the order {@code namen}:
     *  every Jutter ends vrijgesteld as it should; counts in {@code displaced} the firings of the attribution on J2. */
    private static void fireSample(DroolsBackend.Build b, KieBase kieBase, List<String> names, int[] displaced)
            throws Exception {
        {
            Map<Object, String> labels = new IdentityHashMap<>();
            List<Object> beans = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : INVOER.entrySet()) {
                Object beanObject = TmsDocuments.jutterBean(b.beanLoader(), e.getValue());
                labels.put(beanObject, e.getKey());
                beans.add(beanObject);
            }
            Object j2 = beans.get(1);
            TmsDocuments.vuur(b, kieBase, names, beans, sessie -> sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().endsWith("default 1")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == j2))
                        displaced[0]++;
                }
            }), sessie -> { });
            for (Object beanObject : beans)
                assertEquals(VRIJGESTELD.get(labels.get(beanObject)), TmsDocuments.has(beanObject, "vrijgesteld"),
                        () -> labels.get(beanObject) + " vrijgesteld op " + b.branch() + " in volgorde " + names);
        }
    }
}
