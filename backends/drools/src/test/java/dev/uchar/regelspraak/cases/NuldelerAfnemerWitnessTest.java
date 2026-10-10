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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A division by a numeric zero for a consumer that fires a KieBase of {@link DroolsBackend} itself, as the
 * distribution's decision service does: "De verhouding van een Vondst moet berekend worden als zijn gewogen gewicht
 * gedeeld door zijn geschatte gewicht", over V3 with a geschatte gewicht of 0,00 under a gewogen gewicht of 60,00. The
 * KieBase carries Drools' default consequence handler, so such a fire leaves Drools' {@code ConsequenceException},
 * which names the rule and carries the division in its cause chain, and {@code DroolsBackend.benoem} turns it into the
 * sentence the replay gives ({@code NuldelerWitnessTest}); any other consequence fault passes {@code benoem} as it is.
 * A handler of this backend set by class name is instantiated by Drools in every new session, and where the kie API
 * and this backend are loaded by different class loaders that instance does not cast and no session opens; this
 * witness cannot build that class-loader split, so it asserts that no such handler stands between the fire and Drools.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code 385b36a5}, whose KieBases carried
 * {@code DroolsBackend.Nuldelingmelder}): the direct fire threw the named {@code DelingDoorNul} out of that handler
 * instead of Drools' exception, on both branches, and {@code benoem} did not exist.
 *
 * <p>The truth-maintenance branch does not throw out of its fire: it inserts an evaluation fault as a fact, so its
 * consumer fires through {@code DroolsBackend.vuur} to the fixpoint, reads the fault as data with
 * {@code DroolsBackend.lees}, and {@code DroolsBackend.zetAfgeleideWaarden} throws the named {@code DelingDoorNul} there,
 * the sentence the replay gives (the last row; {@code TmsFoutWitnessTest}). The rows of Drools' exception ask the
 * DRL-text branch alone since card tms-omschakeling (2026-10-10); the destructive exec-model branch threw it too.
 */
class NuldelerAfnemerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ZIN = "deling door nul in de regel \"verhouding\": 60 gedeeld door 0 — een numerieke nul als "
            + "deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    /** The branches whose fire throws a fault out of a consequence. */
    private static final List<DroolsBackend.Branch> TAKKEN = List.of(DroolsBackend.Branch.DRL_TEXT);

    @Test void zelf_vuren_geeft_de_uitzondering_van_drools_met_de_regel() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN) {
            ConsequenceException e = assertInstanceOf(ConsequenceException.class, vuurZelf(tak), tak::name);
            assertEquals("verhouding", e.getRule().getName(), tak::name);
            Breuk.DelingDoorNul nul = deling(e);
            assertNotNull(nul, () -> tak + ": geen DelingDoorNul in de keten van " + e);
            assertEquals("Breuk deling door 0", nul.getMessage(), tak::name);
        }
    }

    @Test void benoem_geeft_de_zin_van_de_replay() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN) {
            ConsequenceException e = assertInstanceOf(ConsequenceException.class, vuurZelf(tak), tak::name);
            RuntimeException benoemd = benoem(e);
            assertInstanceOf(Breuk.DelingDoorNul.class, benoemd, tak::name);
            assertEquals(ZIN, benoemd.getMessage(), tak::name);
            // what the consequence threw stays the cause, so the red path's chain reads as the replay's
            assertSame(e.getCause(), benoemd.getCause(), tak::name);
        }
    }

    @Test void de_tak_met_truth_maintenance_geeft_de_zin_bij_het_terugzetten() throws Exception {
        CaseRunner.voorMeting(document(), DroolsBackend.Branch.EXEC_MODEL).metSessie(JSON.readTree(NUL_EN_GEWOON),
                (bouw, sessie, labels) -> {
                    assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "the fire reaches its fixpoint");
                    List<dev.uchar.regelspraak.transpiler.tms.Fout> fouten = DroolsBackend.lees(bouw, sessie).fouten();
                    assertEquals(1, fouten.size(), fouten::toString);
                    assertEquals(ZIN, fouten.get(0).melding(), "the fault as data");
                    assertEquals(ZIN, assertThrows(Breuk.DelingDoorNul.class,
                            () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage(), "the fault thrown");
                    return null;
                });
    }

    @Test void benoem_laat_een_andere_fout_zoals_drools_haar_gaf() throws Exception {
        ConsequenceException e = new ConsequenceException(new IllegalStateException("geen deling"), null, null);
        assertSame(e, benoem(e));
    }

    // ---------------- the fire ----------------

    /** One fire of the case on a KieBase built by {@link DroolsBackend} on {@code tak}, opened and fired the way the
     *  distribution's service does it: the bean loader as the thread's context class loader, every input bean
     *  inserted, one capped {@code fireAllRules}. Returns what left the fire, null if nothing did. */
    private static RuntimeException vuurZelf(DroolsBackend.Branch tak) throws Exception {
        Path ir = document();
        DroolsBackend.Build b = DroolsBackend.build(ir, tak);
        Map<String, Object> beans = CaseRunner.invoerBeans(ir, JSON.readTree(NUL_EN_GEWOON));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
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
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static Breuk.DelingDoorNul deling(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
            if (t instanceof Breuk.DelingDoorNul nul) return nul;
        return null;
    }

    /** {@code DroolsBackend.benoem}, by name, so this witness compiled against the backend before the method existed. */
    private static RuntimeException benoem(ConsequenceException e) throws Exception {
        try {
            return (RuntimeException) DroolsBackend.class.getMethod("benoem", ConsequenceException.class).invoke(null, e);
        } catch (InvocationTargetException fout) {
            throw (Exception) fout.getCause();
        }
    }

    // ---------------- the document and the case ----------------

    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + TWEE_DECIMALEN + "}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** The rule "verhouding": "zijn gewogen gewicht gedeeld door zijn geschatte gewicht", without a condition. */
    private static Path document() throws Exception {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\","
                + "\"attributes\":[" + attribuut("geschatte gewicht") + "," + attribuut("gewogen gewicht") + ","
                + attribuut("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
                + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        String deling = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + eigen("gewogen gewicht") + ",\"right\":"
                + eigen("geschatte gewicht") + ",\"type\":" + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
        String regel = "{\"node\":\"Rule\",\"name\":\"verhouding\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"verhouding\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"De verhouding van een Vondst\"}},\"value\":" + deling + ",\"initialisation\":false,"
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
    private static final String NUL_EN_GEWOON = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
            + vondst("V1", "320.00", "240.00") + "," + vondst("V3", "0.00", "60.00") + "],\"parameters\":{}},\"expected\":[]}";
}
