package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.runtime.Breuk;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.ConsequenceException;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A division by a numeric zero for a consumer that fires a KieBase of {@link DroolsBackend} itself, as the
 * distribution's decision service does: "De verhouding van een Vondst moet berekend worden als zijn gewogen gewicht
 * gedeeld door zijn geschatte gewicht", over V3 with a geschatte gewicht of 0,00 under a gewogen gewicht of 60,00. The
 * KieBase carries Drools' default consequence handler, so a fault that leaves a consequence leaves the fire as Drools'
 * {@code ConsequenceException}, which {@code DroolsBackend.designate} hands on as it is, where it carries no division by
 * zero; an evaluation fault does not leave it on either branch (below).
 * A handler of this backend set by class name is instantiated by Drools in every new session, and where the kie API
 * and this backend are loaded by different class loaders that instance does not cast and no session opens; this
 * witness cannot build that class-loader split, so it asserts that no such handler stands between the fire and Drools.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code 385b36a5}, whose KieBases carried
 * {@code DroolsBackend.Nuldelingmelder}): the direct fire threw the named {@code DelingDoorNul} out of that handler
 * instead of Drools' exception, on both branches, and {@code benoem} did not exist.
 *
 * <p>The analysis with truth maintenance does not throw out of its fire: it inserts an evaluation fault as a fact, so
 * its consumer fires through {@code DroolsBackend.vuur} to the fixpoint, reads the fault as data with
 * {@code DroolsBackend.lees}, and {@code DroolsBackend.zetAfgeleideWaarden} throws the named {@code DelingDoorNul} there,
 * the sentence the replay gives ({@code TmsFaultWitnessTest}). Since card drl-teksttak-op-truth-maintenance that holds on
 * both branches, and a consumer that fires a KieBase itself gets no exception out of the fire at all. The rows of
 * Drools' exception, and of {@code benoem} naming it, asked the DRL-text branch alone from card tms-omschakeling
 * (2026-10-10), when the destructive exec-model branch stopped throwing it, until that card; {@code benoem} stays and
 * passes any other consequence fault as it is.
 */
class ZeroDivisorConsumerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String SENTENCE = "deling door nul in de regel \"verhouding\": 60 gedeeld door 0 — een numerieke nul als "
            + "deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    /** A consumer that fires a KieBase of either branch itself gets nothing out of the fire: the division by zero is
     *  a fact of the fixpoint. Until card drl-teksttak-op-truth-maintenance the DRL-text branch threw Drools'
     *  {@code ConsequenceException} here, naming the rule "verhouding", with "Breuk deling door 0" in its cause chain,
     *  and {@code benoem} turned it into the sentence of the replay. */
    @Test void self_fire_leaves_no_fault_out_of_the_fire() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertNull(fireSelf(branch), branch::name);
    }

    @Test void both_branches_give_the_sentence_on_resetting() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            CaseRunner.forMeasurement(document(), branch).withSession(JSON.readTree(ZERO_AND_ORDINARY), (bouw, sessie, labels) -> {
                assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), branch + ": the fire reaches its fixpoint");
                List<dev.uchar.regelspraak.transpiler.tms.Fout> fouten = DroolsBackend.lees(bouw, sessie).fouten();
                assertEquals(1, fouten.size(), fouten::toString);
                assertEquals(SENTENCE, fouten.get(0).melding(), branch + ": the fault as data");
                assertEquals(SENTENCE, assertThrows(Breuk.DelingDoorNul.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage(), branch + ": the fault thrown");
                return null;
            });
    }

    @Test void designate_lets_another_fault_like_drools_gave_it() throws Exception {
        ConsequenceException e = new ConsequenceException(new IllegalStateException("geen deling"), null, null);
        assertSame(e, designate(e));
    }

    // ---------------- the fire ----------------

    /** One fire of the case on a KieBase built by {@link DroolsBackend} on {@code branch}, opened and fired the way the
     *  distribution's service does it: the bean loader as the thread's context class loader, every input bean
     *  inserted, one capped {@code fireAllRules}. Returns what left the fire, null if nothing did. */
    private static RuntimeException fireSelf(DroolsBackend.Branch branch) throws Exception {
        Path ir = document();
        DroolsBackend.Build b = DroolsBackend.build(ir, branch);
        Map<String, Object> beans = CaseRunner.inputBeans(ir, JSON.readTree(ZERO_AND_ORDINARY));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            for (Object bean : beans.values()) sessie.insert(bean);
            sessie.fireAllRules(100_000);
            return null;
        } catch (RuntimeException e) {
            return e;
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** {@code DroolsBackend.designate}, by name, so this witness compiled against the backend before the method existed. */
    private static RuntimeException designate(ConsequenceException e) throws Exception {
        try {
            return (RuntimeException) DroolsBackend.class.getMethod("designate", ConsequenceException.class).invoke(null, e);
        } catch (InvocationTargetException fout) {
            throw (Exception) fout.getCause();
        }
    }

    // ---------------- the document and the case ----------------

    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + TWO_DECIMALS + "}";
    }

    private static String own(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** The rule "verhouding": "zijn gewogen gewicht gedeeld door zijn geschatte gewicht", without a condition. */
    private static Path document() throws Exception {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\","
                + "\"attributes\":[" + attribuut("geschatte gewicht") + "," + attribuut("gewogen gewicht") + ","
                + attribuut("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
                + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String division = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + own("gewogen gewicht") + ",\"right\":"
                + own("geschatte gewicht") + ",\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
        String regel = "{\"node\":\"Rule\",\"name\":\"verhouding\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"verhouding\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"De verhouding van een Vondst\"}},\"value\":" + division + ",\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}";
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + regel + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                + "\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("nuldeler-afnemer", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    private static String vondst(String label, String geschat, String gewogen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"geschatte gewicht\":{\"number\":\""
                + geschat + "\"},\"gewogen gewicht\":{\"number\":\"" + gewogen + "\"}},\"roles\":{},\"characteristics\":[]}";
    }

    /** V1 (240 by 320) beside V3, whose geschatte gewicht is 0,00 under a gewogen gewicht of 60,00. */
    private static final String ZERO_AND_ORDINARY = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
            + vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", "60.00") + "],\"parameters\":{}},\"expected\":[]}";
}
