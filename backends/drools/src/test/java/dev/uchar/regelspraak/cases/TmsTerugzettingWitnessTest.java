package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.HeeftKenmerk;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.heeft;
import static dev.uchar.regelspraak.cases.TmsDocumenten.jutterBoon;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The write-back of the truth-maintenance branch ({@code DroolsBackend.zetAfgeleideWaarden}). The branch derives with
 * logical inserts and never writes a bean while it fires, so a derived characteristic reaches the bean through this call
 * only, after the fire. "Een Jutter is vergund indien hij erkend is" over J1 (erkend in the input), J2 (vergund in the
 * input, not erkend) and J3 (neither): before the call J1's bean is not vergund, though the session holds the
 * derivation; after it J1 is vergund and still erkend, J2 stays vergund, since input on a characteristic stands and
 * nothing writes false, and J3 carries neither. On the other two branches the call writes nothing: their rules write the
 * beans while they fire.
 *
 * <p><b>Red before the write-back</b> (measured 2026-10-08). On the branch's skeleton the first two rows were refused,
 * "tms-transpiler SKIP: regel: vergund indien erkend — de verlaging van een kenmerktoekenning is nog niet gebouwd". With
 * the lowering in place and the write-back switched off, the replay answered "case: kenmerk vergund: verwacht aanwezig,
 * werkelijk afwezig" and the direct fire failed on "J1 vergund ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;",
 * after the session had shown the HeeftKenmerk of J1. The row on the other two branches was green from the start.
 */
class TmsTerugzettingWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "vergund");

    @Test void de_replay_leest_het_afgeleide_en_de_ingevoerde_kenmerken() throws Exception {
        assertNull(CaseRunner.speelCase(document(), TmsDocumenten.zaak(
                List.of(TmsDocumenten.jutter("J1", List.of("erkend")), TmsDocumenten.jutter("J2", List.of("vergund")),
                        TmsDocumenten.jutter("J3", List.of())),
                List.of(verwacht("J1", "vergund", true), verwacht("J1", "erkend", true),
                        verwacht("J2", "vergund", true), verwacht("J2", "erkend", false),
                        verwacht("J3", "vergund", false), verwacht("J3", "erkend", false))),
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
    }

    @Test void pas_na_het_terugzetten_draagt_de_boon_het_afgeleide_kenmerk() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            Object j1 = jutterBoon(b.beanLoader(), List.of("erkend"));
            Object j2 = jutterBoon(b.beanLoader(), List.of("vergund"));
            Object j3 = jutterBoon(b.beanLoader(), List.of());
            for (Object j : List.of(j1, j2, j3)) sessie.insert(j);
            sessie.fireAllRules(100_000);
            // the derivation is a fact of the session, and the bean does not carry it yet
            List<Object> afgeleid = new ArrayList<>(sessie.getObjects(o -> o instanceof HeeftKenmerk));
            assertEquals(List.of(new HeeftKenmerk(j1, ElementSleutel.van("Jutter", "vergund", true))), afgeleid);
            assertFalse(heeft(j1, "vergund"), "J1 is vergund before the write-back");
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertTrue(heeft(j1, "vergund"), "J1 vergund");
            assertTrue(heeft(j1, "erkend"), "J1 erkend, the input, stays");
            assertTrue(heeft(j2, "vergund"), "J2 vergund, the input, stays");
            assertFalse(heeft(j2, "erkend"), "J2 erkend");
            assertFalse(heeft(j3, "vergund"), "J3 vergund");
            assertFalse(heeft(j3, "erkend"), "J3 erkend");
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** On the exec-model and the DRL-text branch the rule writes J1 vergund while it fires; the witness takes the value
     *  off the bean again and the write-back leaves it so. */
    @Test void op_de_andere_takken_zet_de_terugzetting_niets() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.EXEC_MODEL, DroolsBackend.Branch.DRL_TEXT)) {
            DroolsBackend.Build b = DroolsBackend.build(document(), tak);
            ClassLoader vorige = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(b.beanLoader());
            KieSession sessie = b.kieBase().newKieSession();
            try {
                Object j1 = jutterBoon(b.beanLoader(), List.of("erkend"));
                sessie.insert(j1);
                sessie.fireAllRules(100_000);
                assertTrue(heeft(j1, "vergund"), tak + ": the rule wrote J1 vergund while it fired");
                j1.getClass().getMethod(Namen.setter("vergund"), boolean.class).invoke(j1, false);
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                assertFalse(heeft(j1, "vergund"), tak + ": the write-back wrote J1");
            } finally {
                sessie.dispose();
                Thread.currentThread().setContextClassLoader(vorige);
            }
        }
    }

    private static Path document() throws Exception {
        return TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("vergund indien erkend", "vergund", check("erkend", false))));
    }
}
