package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.KieBaseBuilder;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.conf.EqualityBehaviorOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chain of derived attributes on the exec-model branch, behind a characteristic derived late through a
 * negation: "Een Vondst is geregistreerd indien hij gemeld is", "Een Vondst is onbeheerd indien hij niet geregistreerd
 * is", "Het bergloon van een Vondst moet berekend worden als zijn gewicht maal 2, indien hij onbeheerd is", "De uitkering
 * van een Vondst moet berekend worden als zijn bergloon plus 5". V1 is gemeld (gewicht 10): geregistreerd, not
 * onbeheerd, bergloon leeg, uitkering 5 (a leeg operand of plus counts as 0, RUNTIME-SEMANTICS ch. 1.2). V2 is not
 * (gewicht 10): onbeheerd, bergloon 20, uitkering 25. In an order where the bergloon rule fires on V1 before geregistreerd
 * is derived, V1's bergloon is computed on the transient onbeheerd and the uitkering on that bergloon; when geregistreerd
 * arrives, truth maintenance must withdraw the bergloon and recompute the uitkering, because the uitkering's read of the
 * bergloon is a bound pattern in its left-hand side. Every order of the four rules in the document, and the seeded
 * sample of orders of the generated rule list, at least one of which must take that transient path.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step, whose manifest refused every
 * attribute target): both rows on this branch were refused, "tms-transpiler SKIP: regel: bergloon — een attribuutdoel
 * (berekening van 'bergloon' van Vondst): de truth-maintenance-tak leidt nog alleen kenmerken af". The row on the
 * exec-model branch was green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the uitkering's read of the bergloon taken out
 * of the left-hand side and looked up in working memory from the consequence, the uitkering rule fired once per Vondst
 * before any bergloon stood and never again: all 24 document orders ended wrong, each with "case: attribuut uitkering:
 * verwacht 25, werkelijk 5", the first as "volgorde [registratie, onbeheerd, bergloon, uitkering] ==&gt; expected:
 * &lt;null&gt; but was: &lt;case: attribuut uitkering: verwacht 25, werkelijk 5&gt;", and the sample failed on its first
 * order, "V2 uitkering in volgorde [registratie, onbeheerd, bergloon, uitkering, kenmerkstand Vondst.geregistreerd: waar
 * uit de invoer, …] ==&gt; expected: &lt;0&gt; but was: &lt;1&gt;".
 */
class TmsAttributeChainWitnessTest {

    private static final List<String> RULES = List.of(
            characteristicRule("registratie", "Vondst", "geregistreerd", check("gemeld", false)),
            characteristicRule("onbeheerd", "Vondst", "onbeheerd", check("geregistreerd", true)),
            computingRule("bergloon", "BEREKENING", "Vondst", "bergloon",
                    binary("times", ownRead("gewicht", "Vondst"), number("2")), check("onbeheerd", false)),
            computingRule("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binary("plus", ownRead("bergloon", "Vondst"), number("5")), "null"));

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
            List.of(attribuut("gewicht", WHOLE), attribuut("bergloon", WHOLE), attribuut("uitkering", WHOLE)),
            List.of("gemeld", "geregistreerd", "onbeheerd"))), List.of());

    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    /** Four rules, the characteristic standings of geregistreerd and onbeheerd (three rules each) and the attribute
     *  standing of bergloon (two); gewicht is input and read off the bean. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocuments.document(MODEL, RULES),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        assertEquals(12, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
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
        int throughTheIntermediateState = 0;
        for (List<Rule> order : orders) {
            ModelImpl m = new ModelImpl(b.execModelTranspiler().model().getName());
            order.forEach(m::addRule);
            b.execModelTranspiler().model().getGlobals().forEach(m::addGlobal);
            KieBase kb = KieBaseBuilder.createKieBaseFromModel(m, EqualityBehaviorOption.EQUALITY);
            if (vuur(b, kb, order.stream().map(Rule::getName).toList())) throughTheIntermediateState++;
        }
        assertTrue(throughTheIntermediateState > 0, "geen volgorde berekende het bergloon van V1");
    }

    /** Fires V1 and V2 in {@code kb}, writes back, checks the beans, and says whether the bergloon rule fired on V1, whose
     *  onbeheerd is onwaar at the end: that firing is the transient path. */
    private static boolean vuur(DroolsBackend.Build b, KieBase kb, List<String> names) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kb.newKieSession();
        try {
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "10"), List.of("gemeld"));
            Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "10"), List.of());
            boolean[] intermediateState = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("bergloon")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v1))
                        intermediateState[0] = true;
                }
            });
            sessie.insert(v1);
            sessie.insert(v2);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), () -> "geen fixpunt in volgorde " + names);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            String where = " in volgorde " + names;
            assertNull(TmsDocuments.lees(v1, "bergloon"), "V1 bergloon" + where);
            assertEquals(0, new BigDecimal("5").compareTo((BigDecimal) TmsDocuments.lees(v1, "uitkering")), "V1 uitkering" + where);
            assertEquals(0, new BigDecimal("20").compareTo((BigDecimal) TmsDocuments.lees(v2, "bergloon")), "V2 bergloon" + where);
            assertEquals(0, new BigDecimal("25").compareTo((BigDecimal) TmsDocuments.lees(v2, "uitkering")), "V2 uitkering" + where);
            assertEquals(List.of(true, false), List.of(TmsDocuments.has(v1, "geregistreerd"), TmsDocuments.has(v1, "onbeheerd")), "V1" + where);
            assertEquals(List.of(false, true), List.of(TmsDocuments.has(v2, "geregistreerd"), TmsDocuments.has(v2, "onbeheerd")), "V2" + where);
            return intermediateState[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static JsonNode caseDocument() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(TmsDocuments.instance("V1", "Vondst", Map.of("gewicht", "10"), List.of("gemeld"), Map.of()),
                        TmsDocuments.instance("V2", "Vondst", Map.of("gewicht", "10"), List.of(), Map.of())),
                List.of(verwacht("V1", "geregistreerd", true), verwacht("V1", "onbeheerd", false),
                        waarde("V1", "bergloon", null), waarde("V1", "uitkering", "5"),
                        verwacht("V2", "geregistreerd", false), verwacht("V2", "onbeheerd", true),
                        waarde("V2", "bergloon", "20"), waarde("V2", "uitkering", "25")));
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
