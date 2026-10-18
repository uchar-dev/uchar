package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.KieBaseBuilder;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.runtime.KieSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chain of two negations on the exec-model branch: "Een Jutter is vergund indien hij erkend is", "Een Jutter
 * is onvergund indien hij niet vergund is", "Een Jutter is bevoegd indien hij niet onvergund is". In the first document
 * erkend is case input; in the second it is derived itself, "Een Jutter is erkend indien hij aangemeld is". The strata
 * are 0, 1, 2 (one more where erkend is derived, RUNTIME-SEMANTICS ch. 6.4): J1, erkend, ends vergund, not onvergund,
 * bevoegd; J2, not erkend, ends not vergund, onvergund, not bevoegd. The exec-model branch reaches this by placing each
 * negated read below its producers; this branch places nothing, so its answer must not depend on any order: every order
 * of the rules in the document, and a fixed, seeded sample of orders of the complete list of rules the transpiler
 * generates, the standing rules among them, each fired in a KieBase of its own in EQUALITY mode.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's skeleton, whose manifest listed every
 * characteristic attribution as not lowered): every row was refused, "tms-transpiler SKIP: regel: vergund indien erkend
 * — de verlaging van een kenmerktoekenning is nog niet gebouwd" (with erkend derived: "… regel: erkend indien aangemeld
 * — …"). With the lowering in place and the write-back to the beans still switched off, the order [vergund indien erkend,
 * onvergund indien niet vergund, bevoegd indien niet onvergund] answered "case: kenmerk vergund: verwacht aanwezig,
 * werkelijk afwezig; kenmerk bevoegd: verwacht aanwezig, werkelijk afwezig; kenmerk onvergund: verwacht aanwezig,
 * werkelijk afwezig", the derived document added "kenmerk erkend: verwacht aanwezig, werkelijk afwezig", and the sample
 * failed on "J1 bevoegd … expected: &lt;true&gt; but was: &lt;false&gt;": the derivations stood in the session and no
 * bean carried them.
 *
 * <p><b>Red by mutation</b> (measured 2026-10-08 on the lowering, not committed): what the rows catch is a derivation
 * that stays when its condition stops holding. With the characteristic rule inserting its {@code HasCharacteristic} plainly
 * instead of logically, 3 of the 6 orders of the document with erkend as input ended wrong, the three with "bevoegd
 * indien niet onvergund" before "onvergund indien niet vergund", the first of them failing "volgorde [vergund indien
 * erkend, bevoegd indien niet onvergund, onvergund indien niet vergund] ==&gt; expected: &lt;null&gt; but was:
 * &lt;case: kenmerk bevoegd: verwacht afwezig, werkelijk aanwezig&gt;"; 20 of the 24 orders with erkend derived ended
 * wrong, some also with "kenmerk onvergund: verwacht afwezig, werkelijk aanwezig"; and of the 102 orders of the
 * generated rules 72 and 92 ended wrong, failing "J2 bevoegd in volgorde […] ==&gt; expected: &lt;false&gt; but was:
 * &lt;true&gt;". With the onwaar standing inserted plainly instead, the same 3 and 20 document orders ended wrong, and 86
 * and 98 of the 102 generated orders, failing "J1 onvergund in volgorde […] ==&gt; expected: &lt;false&gt; but was:
 * &lt;true&gt;". Every row of this witness failed on both mutants.
 */
class TmsNegationChainWitnessTest {

    private static final List<String> CHARACTERISTICS = List.of("aangemeld", "erkend", "vergund", "onvergund", "bevoegd");

    private static final String ERKEND = characteristicRule("erkend indien aangemeld", "erkend", check("aangemeld", false));
    private static final String VERGUND = characteristicRule("vergund indien erkend", "vergund", check("erkend", false));
    private static final String UNPERMITTED = characteristicRule("onvergund indien niet vergund", "onvergund", check("vergund", true));
    private static final String BEVOEGD = characteristicRule("bevoegd indien niet onvergund", "bevoegd", check("onvergund", true));

    /** erkend as case input: three rules. */
    private static final List<String> INVOER = List.of(VERGUND, UNPERMITTED, BEVOEGD);
    /** erkend derived from aangemeld: four rules. */
    private static final List<String> DERIVED = List.of(ERKEND, VERGUND, UNPERMITTED, BEVOEGD);

    // ---------------- every order of the rules in the document ----------------

    @Test void every_order_of_the_rules_with_erkend_as_input() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(INVOER)) {
            List<String> names = names(order);
            assertNull(play(order, "erkend"), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    @Test void every_order_of_the_rules_with_erkend_derived() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(DERIVED)) {
            List<String> names = names(order);
            assertNull(play(order, "aangemeld"), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    // ---------------- a seeded sample of orders of the generated rules ----------------

    /** Three rules of the document, and the three standing rules of each derived characteristic a condition reads
     *  (vergund, onvergund); erkend is input and read off the bean. */
    @Test void sample_of_orders_of_the_generated_rules_with_erkend_as_input() throws Exception {
        sample(document(INVOER), "erkend", 9);
    }

    /** Four rules and the standing rules of erkend, vergund and onvergund. */
    @Test void sample_of_orders_of_the_generated_rules_with_erkend_derived() throws Exception {
        sample(document(DERIVED), "aangemeld", 13);
    }

    /** The generated rule list in its own order, reversed, and in 100 orders drawn with a fixed seed, each in a fresh
     *  KieBase in EQUALITY mode, fired to its fixpoint under the replay's cap and written back as the replay does. */
    private static void sample(Path ir, String input, int rules) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> generated = b.execModelTranspiler().model().getRules();
        assertEquals(rules, generated.size(), () -> "gegenereerde regels: " + generated.stream().map(Rule::getName).toList());
        List<List<Rule>> orders = new ArrayList<>();
        orders.add(generated);
        List<Rule> reversed = new ArrayList<>(generated);
        Collections.reverse(reversed);
        orders.add(reversed);
        Random chance = new Random(20261008L);
        for (int i = 0; i < 100; i++) {
            List<Rule> v = new ArrayList<>(generated);
            Collections.shuffle(v, chance);
            orders.add(v);
        }
        for (List<Rule> order : orders) {
            ModelImpl m = new ModelImpl(b.execModelTranspiler().model().getName());
            order.forEach(m::addRule);
            b.execModelTranspiler().model().getGlobals().forEach(m::addGlobal);
            KieBase kb = KieBaseBuilder.createKieBaseFromModel(m, EqualityBehaviorOption.EQUALITY);
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(b.beanLoader());
            KieSession session = kb.newKieSession();
            try {
                Object j1 = TmsDocuments.jutterBean(b.beanLoader(), List.of(input));
                Object j2 = TmsDocuments.jutterBean(b.beanLoader(), List.of());
                session.insert(j1);
                session.insert(j2);
                DroolsBackend.Firing firing = DroolsBackend.fire(b, session, 100_000);
                List<String> names = order.stream().map(Rule::getName).toList();
                assertNull(firing.noFixpoint(), () -> "geen fixpunt in volgorde " + names);
                DroolsBackend.setDerivedValues(b, session);
                Map<String, Boolean> end1 = Map.of("erkend", true, "vergund", true, "onvergund", false, "bevoegd", true);
                Map<String, Boolean> end2 = Map.of("erkend", false, "vergund", false, "onvergund", true, "bevoegd", false);
                for (String k : end1.keySet()) {
                    assertEquals(end1.get(k), TmsDocuments.has(j1, k), () -> "J1 " + k + " in volgorde " + names);
                    assertEquals(end2.get(k), TmsDocuments.has(j2, k), () -> "J2 " + k + " in volgorde " + names);
                }
            } finally {
                session.dispose();
                Thread.currentThread().setContextClassLoader(previous);
            }
        }
    }

    // ---------------- the documents and the case ----------------

    private static String play(List<String> order, String input) throws Exception {
        return CaseRunner.playCase(document(order), caseDocument(input), DroolsBackend.Branch.EXEC_MODEL);
    }

    private static Path document(List<String> rules) throws Exception {
        return TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS), rules);
    }

    /** J1 with {@code input} set, J2 with nothing; erkend ends present on J1 in both documents. */
    private static JsonNode caseDocument(String input) throws Exception {
        return TmsDocuments.caseDocument(
                List.of(TmsDocuments.jutter("J1", List.of(input)), TmsDocuments.jutter("J2", List.of())),
                List.of(expected("J1", "erkend", true), expected("J1", "vergund", true),
                        expected("J1", "onvergund", false), expected("J1", "bevoegd", true),
                        expected("J2", "erkend", false), expected("J2", "vergund", false),
                        expected("J2", "onvergund", true), expected("J2", "bevoegd", false)));
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
