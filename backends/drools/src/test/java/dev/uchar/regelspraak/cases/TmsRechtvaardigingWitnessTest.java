package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.HeeftKenmerk;

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

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.heeft;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One logical fact with two justifications on the truth-maintenance branch, and what remains of it when one of them
 * falls. "Een Jutter is vergund indien hij erkend is" beside "Een Jutter is vergund indien hij niet geblokkeerd is", with
 * "Een Jutter is geblokkeerd indien hij geschorst is"; erkend and geschorst are case input. J1 is erkend and geschorst:
 * in an order where the second rule fires before geblokkeerd is derived, both rules justify J1's vergund, and when
 * geblokkeerd arrives the second justification falls and vergund stays on the first. J2 is geschorst only and ends
 * without vergund, J3 has neither and is vergund on the second rule, J4 is erkend only and vergund on both. Truth
 * maintenance merges the two inserts by the equality of the fact class, whatever the KieBase's equality mode; this
 * witness pins what the branch relies on from that merge: at the end one {@code HeeftKenmerk} per object and
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
 * equality of the fact class, and both mutants of it turn this witness red. With {@code HeeftKenmerk.hashCode} taken
 * from the object's identity, equal facts did not merge: "J4 in documentvolgorde [vergund indien erkend, vergund indien
 * niet geblokkeerd, geblokkeerd indien geschorst]: [HeeftKenmerk(Jutter@…, jutter.vergund), HeeftKenmerk(Jutter@…,
 * jutter.vergund)] ==&gt; expected: &lt;1&gt; but was: &lt;2&gt;". With {@code equals} and {@code hashCode} ignoring the
 * subject, the facts of different Jutters merged into one fact, which the write-back put on one bean only: the replay
 * answered "case: kenmerk vergund: verwacht aanwezig, werkelijk afwezig; kenmerk vergund: verwacht aanwezig, werkelijk
 * afwezig", and the sample failed on "J1 in volgorde […] ==&gt; expected: &lt;1&gt; but was: &lt;5&gt;", five
 * justifications on one fact.
 */
class TmsRechtvaardigingWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "geschorst", "geblokkeerd", "vergund");
    private static final String NIET_GEBLOKKEERD = "vergund indien niet geblokkeerd";
    private static final List<String> REGELS = List.of(
            kenmerkregel("vergund indien erkend", "vergund", check("erkend", false)),
            kenmerkregel(NIET_GEBLOKKEERD, "vergund", check("geblokkeerd", true)),
            kenmerkregel("geblokkeerd indien geschorst", "geblokkeerd", check("geschorst", false)));

    /** J1 to J4: their input, and per object whether it ends vergund and how many rules justify it then. */
    private static final List<List<String>> INVOER = List.of(List.of("erkend", "geschorst"), List.of("geschorst"),
            List.of(), List.of("erkend"));
    private static final boolean[] VERGUND = {true, false, true, true};
    private static final int[] RECHTVAARDIGINGEN = {1, 0, 1, 2};

    /** In every document order the generated model puts the three rules before the standing rules, and geblokkeerd is
     *  derived before its onwaar standing can be inserted, so none of these orders fires the rule on "niet geblokkeerd"
     *  on J1; the path through the falling justification is the sample's below. */
    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = new ArrayList<>();
            for (String r : volgorde) namen.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
            Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), volgorde);
            assertNull(CaseRunner.speelCase(ir, zaak(), DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + namen);
            DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
            vuur(b, b.kieBase(), "documentvolgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), REGELS),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
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
        int doorDeVallendeRechtvaardiging = 0;
        for (List<Rule> volgorde : volgordes) {
            ModelImpl m = new ModelImpl(b.execModelTranspiler().model().getName());
            volgorde.forEach(m::addRule);
            KieBase kb = KieBaseBuilder.createKieBaseFromModel(m, EqualityBehaviorOption.EQUALITY);
            if (vuur(b, kb, "volgorde " + volgorde.stream().map(Rule::getName).toList())) doorDeVallendeRechtvaardiging++;
        }
        assertTrue(doorDeVallendeRechtvaardiging > 0, "geen volgorde vuurde '" + NIET_GEBLOKKEERD + "' op J1");
    }

    /** Fires the four Jutters in {@code kb}, checks the facts and their justifications in the session and the beans after
     *  the write-back, and says whether the rule on "niet geblokkeerd" fired on J1, whose geblokkeerd holds at the end:
     *  that firing is the path through the falling justification. */
    private static boolean vuur(DroolsBackend.Build b, KieBase kb, String waar) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kb.newKieSession();
        try {
            List<Object> jutters = new ArrayList<>();
            for (List<String> invoer : INVOER) jutters.add(TmsDocumenten.jutterBoon(b.beanLoader(), invoer));
            boolean[] viaNiet = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals(NIET_GEBLOKKEERD)
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == jutters.get(0)))
                        viaNiet[0] = true;
                }
            });
            for (Object j : jutters) sessie.insert(j);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), () -> "geen fixpunt in " + waar);
            for (int i = 0; i < jutters.size(); i++) {
                Object j = jutters.get(i);
                List<Object> feiten = new ArrayList<>(sessie.getObjects(o -> o instanceof HeeftKenmerk h
                        && h.onderwerp() == j && h.kenmerk().naam().equals("vergund")));
                String wie = "J" + (i + 1) + " in " + waar;
                assertEquals(VERGUND[i] ? 1 : 0, feiten.size(), () -> wie + ": " + feiten);
                if (VERGUND[i]) assertEquals(RECHTVAARDIGINGEN[i], rechtvaardigingen(sessie, feiten.get(0)), wie);
            }
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            for (int i = 0; i < jutters.size(); i++)
                assertEquals(VERGUND[i], heeft(jutters.get(i), "vergund"), "J" + (i + 1) + " vergund in " + waar);
            return viaNiet[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** How many activations justify the logical fact {@code feit}: the size of its belief set. */
    private static int rechtvaardigingen(KieSession sessie, Object feit) {
        InternalFactHandle h = (InternalFactHandle) sessie.getFactHandle(feit);
        return ((TruthMaintenanceSystemEqualityKey) h.getEqualityKey()).getBeliefSet().size();
    }

    private static JsonNode zaak() throws Exception {
        List<String> jutters = new ArrayList<>(), verwacht = new ArrayList<>();
        for (int i = 0; i < INVOER.size(); i++) {
            jutters.add(TmsDocumenten.jutter("J" + (i + 1), INVOER.get(i)));
            verwacht.add(verwacht("J" + (i + 1), "vergund", VERGUND[i]));
        }
        return TmsDocumenten.zaak(jutters, verwacht);
    }
}
