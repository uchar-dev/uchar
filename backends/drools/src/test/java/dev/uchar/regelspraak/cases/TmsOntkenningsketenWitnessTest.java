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

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A chain of two negations on the truth-maintenance branch: "Een Jutter is vergund indien hij erkend is", "Een Jutter
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
 * that stays when its condition stops holding. With the characteristic rule inserting its {@code HeeftKenmerk} plainly
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
class TmsOntkenningsketenWitnessTest {

    private static final List<String> KENMERKEN = List.of("aangemeld", "erkend", "vergund", "onvergund", "bevoegd");

    private static final String ERKEND = kenmerkregel("erkend indien aangemeld", "erkend", check("aangemeld", false));
    private static final String VERGUND = kenmerkregel("vergund indien erkend", "vergund", check("erkend", false));
    private static final String ONVERGUND = kenmerkregel("onvergund indien niet vergund", "onvergund", check("vergund", true));
    private static final String BEVOEGD = kenmerkregel("bevoegd indien niet onvergund", "bevoegd", check("onvergund", true));

    /** erkend as case input: three rules. */
    private static final List<String> INVOER = List.of(VERGUND, ONVERGUND, BEVOEGD);
    /** erkend derived from aangemeld: four rules. */
    private static final List<String> AFGELEID = List.of(ERKEND, VERGUND, ONVERGUND, BEVOEGD);

    // ---------------- every order of the rules in the document ----------------

    @Test void elke_volgorde_van_de_regels_met_erkend_als_invoer() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(INVOER)) {
            List<String> namen = namen(volgorde);
            assertNull(speel(volgorde, "erkend"), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    @Test void elke_volgorde_van_de_regels_met_erkend_afgeleid() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(AFGELEID)) {
            List<String> namen = namen(volgorde);
            assertNull(speel(volgorde, "aangemeld"), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    /** The pinned end state is the one the gate's branch answers on the same documents. */
    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(document(INVOER), zaak("erkend"), DroolsBackend.Branch.EXEC_MODEL));
        assertNull(CaseRunner.speelCase(document(AFGELEID), zaak("aangemeld"), DroolsBackend.Branch.EXEC_MODEL));
    }

    // ---------------- a seeded sample of orders of the generated rules ----------------

    /** Three rules of the document, and the three standing rules of each derived characteristic a condition reads
     *  (vergund, onvergund); erkend is input and read off the bean. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels_met_erkend_als_invoer() throws Exception {
        steekproef(document(INVOER), "erkend", 9);
    }

    /** Four rules and the standing rules of erkend, vergund and onvergund. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels_met_erkend_afgeleid() throws Exception {
        steekproef(document(AFGELEID), "aangemeld", 13);
    }

    /** The generated rule list in its own order, reversed, and in 100 orders drawn with a fixed seed, each in a fresh
     *  KieBase in EQUALITY mode, fired to its fixpoint under the replay's cap and written back as the replay does. */
    private static void steekproef(Path ir, String invoer, int regels) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(regels, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        List<List<Rule>> volgordes = new ArrayList<>();
        volgordes.add(gegenereerd);
        List<Rule> omgekeerd = new ArrayList<>(gegenereerd);
        Collections.reverse(omgekeerd);
        volgordes.add(omgekeerd);
        Random toeval = new Random(20261008L);
        for (int i = 0; i < 100; i++) {
            List<Rule> v = new ArrayList<>(gegenereerd);
            Collections.shuffle(v, toeval);
            volgordes.add(v);
        }
        for (List<Rule> volgorde : volgordes) {
            ModelImpl m = new ModelImpl(b.tmsTranspiler().model().getName());
            volgorde.forEach(m::addRule);
            b.tmsTranspiler().model().getGlobals().forEach(m::addGlobal);
            KieBase kb = KieBaseBuilder.createKieBaseFromModel(m, EqualityBehaviorOption.EQUALITY);
            ClassLoader vorige = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(b.beanLoader());
            KieSession sessie = kb.newKieSession();
            try {
                Object j1 = TmsDocumenten.jutterBoon(b.beanLoader(), List.of(invoer));
                Object j2 = TmsDocumenten.jutterBoon(b.beanLoader(), List.of());
                sessie.insert(j1);
                sessie.insert(j2);
                DroolsBackend.Vuring vuring = DroolsBackend.vuur(b, sessie, 100_000);
                List<String> namen = volgorde.stream().map(Rule::getName).toList();
                assertNull(vuring.geenFixpunt(), () -> "geen fixpunt in volgorde " + namen);
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                Map<String, Boolean> eind1 = Map.of("erkend", true, "vergund", true, "onvergund", false, "bevoegd", true);
                Map<String, Boolean> eind2 = Map.of("erkend", false, "vergund", false, "onvergund", true, "bevoegd", false);
                for (String k : eind1.keySet()) {
                    assertEquals(eind1.get(k), TmsDocumenten.heeft(j1, k), () -> "J1 " + k + " in volgorde " + namen);
                    assertEquals(eind2.get(k), TmsDocumenten.heeft(j2, k), () -> "J2 " + k + " in volgorde " + namen);
                }
            } finally {
                sessie.dispose();
                Thread.currentThread().setContextClassLoader(vorige);
            }
        }
    }

    // ---------------- the documents and the case ----------------

    private static String speel(List<String> volgorde, String invoer) throws Exception {
        return CaseRunner.speelCase(document(volgorde), zaak(invoer), DroolsBackend.Branch.TRUTH_MAINTENANCE);
    }

    private static Path document(List<String> regels) throws Exception {
        return TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), regels);
    }

    /** J1 with {@code invoer} set, J2 with nothing; erkend ends present on J1 in both documents. */
    private static JsonNode zaak(String invoer) throws Exception {
        return TmsDocumenten.zaak(
                List.of(TmsDocumenten.jutter("J1", List.of(invoer)), TmsDocumenten.jutter("J2", List.of())),
                List.of(verwacht("J1", "erkend", true), verwacht("J1", "vergund", true),
                        verwacht("J1", "onvergund", false), verwacht("J1", "bevoegd", true),
                        verwacht("J2", "erkend", false), verwacht("J2", "vergund", false),
                        verwacht("J2", "onvergund", true), verwacht("J2", "bevoegd", false)));
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
