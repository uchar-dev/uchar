package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.HasCharacteristic;

import org.drools.core.common.InternalFactHandle;
import org.drools.model.Rule;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.KieBaseBuilder;
import org.drools.tms.TruthMaintenanceSystemEqualityKey;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.has;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One logical fact with two justifications on the exec-model branch, and what remains of it when one of them
 * falls. "Een Jutter is vergund indien hij erkend is" beside "Een Jutter is vergund indien hij niet geblokkeerd is", with
 * "Een Jutter is geblokkeerd indien hij geschorst is"; erkend and geschorst are case input. J1 is erkend and geschorst:
 * in an order where the second rule fires before geblokkeerd is derived, both rules justify J1's vergund, and when
 * geblokkeerd arrives the second justification falls and vergund stays on the first. J2 is geschorst only and ends
 * without vergund, J3 has neither and is vergund on the second rule, J4 is erkend only and vergund on both. Truth
 * maintenance merges the two inserts by the equality of the fact class, whatever the KieBase's equality mode; this
 * witness pins what the branch relies on from that merge: at the end one {@code HasCharacteristic} per object and
 * characteristic, a justification for each rule whose condition holds (two for J4, one for J1 and J3), and the
 * characteristic on the bean after the write-back. Every order of the three rules in the document, and the seeded sample
 * of orders of the generated rule list; at least one of those orders must take the path through J1's falling
 * justification.
 *
 * <p>Measured 2026-10-08: no document order takes the path through the falling justification (the generated model puts
 * the three rules before the standing rules, and geblokkeerd is derived before its onwaar standing can be inserted);
 * 36 of the 102 orders of the generated rule list take it.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). The merge and the justification per rule are the
 * engine's bookkeeping: the branch inserts logically, and the write-back and the standing read what the session holds,
 * so no reasonable mutant of either loses a merged fact. What the merge rests on in the branch's own code is the
 * equality of the fact class, and both mutants of it turn this witness red. With {@code HasCharacteristic.hashCode} taken
 * from the object's identity, equal facts did not merge: "J4 in documentvolgorde [vergund indien erkend, vergund indien
 * niet geblokkeerd, geblokkeerd indien geschorst]: [HasCharacteristic(Jutter@…, jutter.vergund), HasCharacteristic(Jutter@…,
 * jutter.vergund)] ==&gt; expected: &lt;1&gt; but was: &lt;2&gt;". With {@code equals} and {@code hashCode} ignoring the
 * subject, the facts of different Jutters merged into one fact, which the write-back put on one bean only: the replay
 * answered "case: kenmerk vergund: verwacht aanwezig, werkelijk afwezig; kenmerk vergund: verwacht aanwezig, werkelijk
 * afwezig", and the sample failed on "J1 in volgorde […] ==&gt; expected: &lt;1&gt; but was: &lt;5&gt;", five
 * justifications on one fact.
 */
class TmsJustificationWitnessTest {

    private static final List<String> CHARACTERISTICS = List.of("erkend", "geschorst", "geblokkeerd", "vergund");
    private static final String NON_BLOCKED = "vergund indien niet geblokkeerd";
    private static final List<String> RULES = List.of(
            characteristicRule("vergund indien erkend", "vergund", check("erkend", false)),
            characteristicRule(NON_BLOCKED, "vergund", check("geblokkeerd", true)),
            characteristicRule("geblokkeerd indien geschorst", "geblokkeerd", check("geschorst", false)));

    /** J1 to J4: their input, and per object whether it ends vergund and how many rules justify it then. */
    private static final List<List<String>> INVOER = List.of(List.of("erkend", "geschorst"), List.of("geschorst"),
            List.of(), List.of("erkend"));
    private static final boolean[] VERGUND = {true, false, true, true};
    private static final int[] JUSTIFICATIONS = {1, 0, 1, 2};

    /** In every document order the generated model puts the three rules before the standing rules, and geblokkeerd is
     *  derived before its onwaar standing can be inserted, so none of these orders fires the rule on "niet geblokkeerd"
     *  on J1; the path through the falling justification is the sample's below. */
    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = new ArrayList<>();
            for (String r : order) names.add(TmsDocuments.JSON.readTree(r).get("name").asText());
            Path ir = TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS), order);
            assertNull(CaseRunner.playCase(ir, caseDocument(), DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            vuur(b, b.kieBase(), "documentvolgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS), RULES),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        List<List<Rule>> orders = new ArrayList<>();
        orders.add(gegenereerd);
        List<Rule> reversed = new ArrayList<>(gegenereerd);
        Collections.reverse(reversed);
        orders.add(reversed);
        Random chance = new Random(20261008L);
        for (int i = 0; i < 100; i++) {
            List<Rule> v = new ArrayList<>(gegenereerd);
            Collections.shuffle(v, chance);
            orders.add(v);
        }
        int byTheFallingJustification = 0;
        for (List<Rule> order : orders) {
            ModelImpl m = new ModelImpl(b.execModelTranspiler().model().getName());
            order.forEach(m::addRule);
            KieBase kb = KieBaseBuilder.createKieBaseFromModel(m, EqualityBehaviorOption.EQUALITY);
            if (vuur(b, kb, "volgorde " + order.stream().map(Rule::getName).toList())) byTheFallingJustification++;
        }
        assertTrue(byTheFallingJustification > 0, "geen volgorde vuurde '" + NON_BLOCKED + "' op J1");
    }

    /** Fires the four Jutters in {@code kb}, checks the facts and their justifications in the session and the beans after
     *  the write-back, and says whether the rule on "niet geblokkeerd" fired on J1, whose geblokkeerd holds at the end:
     *  that firing is the path through the falling justification. */
    private static boolean vuur(DroolsBackend.Build b, KieBase kb, String where) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kb.newKieSession();
        try {
            List<Object> jutters = new ArrayList<>();
            for (List<String> invoer : INVOER) jutters.add(TmsDocuments.jutterBean(b.beanLoader(), invoer));
            boolean[] viaNot = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals(NON_BLOCKED)
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == jutters.get(0)))
                        viaNot[0] = true;
                }
            });
            for (Object j : jutters) sessie.insert(j);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), () -> "geen fixpunt in " + where);
            for (int i = 0; i < jutters.size(); i++) {
                Object j = jutters.get(i);
                List<Object> sessionFacts = new ArrayList<>(sessie.getObjects(o -> o instanceof HasCharacteristic h
                        && h.onderwerp() == j && h.kenmerk().naam().equals("vergund")));
                String formName = "J" + (i + 1) + " in " + where;
                assertEquals(VERGUND[i] ? 1 : 0, sessionFacts.size(), () -> formName + ": " + sessionFacts);
                if (VERGUND[i]) assertEquals(JUSTIFICATIONS[i], justifications(sessie, sessionFacts.get(0)), formName);
            }
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            for (int i = 0; i < jutters.size(); i++)
                assertEquals(VERGUND[i], has(jutters.get(i), "vergund"), "J" + (i + 1) + " vergund in " + where);
            return viaNot[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** How many activations justify the logical fact {@code feit}: the size of its belief set. */
    private static int justifications(KieSession sessie, Object fact) {
        InternalFactHandle h = (InternalFactHandle) sessie.getFactHandle(fact);
        return ((TruthMaintenanceSystemEqualityKey) h.getEqualityKey()).getBeliefSet().size();
    }

    private static JsonNode caseDocument() throws Exception {
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < INVOER.size(); i++) {
            jutters.add(TmsDocuments.jutter("J" + (i + 1), INVOER.get(i)));
            verwacht.add(verwacht("J" + (i + 1), "vergund", VERGUND[i]));
        }
        return TmsDocuments.caseDocument(jutters, verwacht);
    }
}
