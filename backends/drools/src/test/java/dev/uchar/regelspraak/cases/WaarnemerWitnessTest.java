package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.engine.ScenarioResultaat;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;

import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a session fired, through the backend's public entry: {@code DroolsBackend.vuur(build, session, waarnemer)} with
 * a {@code DroolsBackend.Waarnemer} whose labels the caller gives, and {@code DroolsBackend.dekking(build, forms)}. A
 * consumer that fires a session itself, as the distribution's decision service and scenario runner do, gets the trace
 * and the coverage the scenario report gives ({@code CaseRunner.rapport}, which uses the same entry; its rows are
 * pinned in {@code ScenarioRapportWitnessTest}) without a copy of the report's listener. Every session here is set up as
 * a consumer sets it up, through {@code CaseRunner.metSessie} or with its own beans and {@code DroolsBackend.voegIn},
 * and fired through the entry alone.
 *
 * <p>The rows pin what the report's rows cannot see: that two sessions of one build, fired at once on two threads,
 * each with a waarnemer of its own, see only their own firings; that the waarnemer's listener is on the session during
 * {@code vuur} alone, so a firing outside it is not counted, also after a {@code vuur} that refuses the session; and the
 * two refusals, of a waarnemer that observed another session and of a fired form outside the manifest.
 */
class WaarnemerWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path UITSLUITING = GOLDEN.resolve("jutterswet-uitsluiting-rgs");
    /** An object creation: the session fires in generations ({@code DroolsBackend.vuur(Build, KieSession, int)}). */
    private static final Path WRAKBOEKING = GOLDEN.resolve("jutterswet-wrakboeking");

    /** What a waarnemer recorded, rendered for comparison: per row "form [labels] count", the exact forms, and per
     *  manifest entry "name SOORT". */
    private record Waargenomen(List<String> spoor, Set<String> vormen, List<String> dekking) {}

    private static Waargenomen waargenomen(DroolsBackend.Build bouw, DroolsBackend.Waarnemer w) {
        List<String> spoor = new ArrayList<>();
        for (ScenarioResultaat.Vuring v : w.vuringen()) spoor.add(v.regel() + " " + v.labels() + " " + v.aantal());
        List<String> dekking = new ArrayList<>();
        for (ScenarioResultaat.DekkingRegel d : DroolsBackend.dekking(bouw, w.vormen())) dekking.add(d.regel() + " " + d.soort());
        return new Waargenomen(spoor, w.vormen(), dekking);
    }

    /** The label of an input bean by identity in {@code labels}, null for any other object. */
    private static java.util.function.Function<Object, String> labelVan(Map<String, Object> labels) {
        return o -> {
            for (Map.Entry<String, Object> e : labels.entrySet()) if (e.getValue() == o) return e.getKey();
            return null;
        };
    }

    /** Case {@code casus} of {@code golden} on {@code tak}, set up as the replay sets it up and fired through the entry
     *  with a waarnemer over the case labels. */
    private static Waargenomen speel(Path golden, String casus, DroolsBackend.Branch tak) throws Exception {
        JsonNode c = TmsDocumenten.JSON.readTree(golden.resolve(casus).toFile());
        return CaseRunner.voorMeting(golden.resolve("ir.json"), tak).metSessie(c, (bouw, sessie, labels) -> {
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelVan(labels));
            assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt(), tak + ": the fire reaches its fixpoint");
            return waargenomen(bouw, w);
        });
    }

    private static ScenarioResultaat rapport(Path golden, String casus) {
        return CaseRunner.rapport(golden.resolve("ir.json"), List.of(golden.resolve(casus))).get(0);
    }

    @Test void de_waarnemer_geeft_op_beide_takken_wat_het_rapport_geeft() throws Exception {
        for (DroolsBackend.Branch tak : DroolsBackend.Branch.values()) {
            Waargenomen geweigerd = speel(UITSLUITING, "case1-geweigerd.json", tak);
            assertEquals(List.of("defaults K:Jutter.uitgesloten [M] 2"), geweigerd.spoor(),
                    tak + ": de defaultsgroep vuurt twee keer op M; de stand van het kenmerk heeft geen rij");
            assertEquals(Set.of("defaults K:Jutter.uitgesloten"), geweigerd.vormen(), tak::name);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten NIET_GERAAKT", "defaults K:Jutter.uitgesloten GEVUURD"),
                    geweigerd.dekking(), tak + ": één regel per post van het manifest, in zijn volgorde");
            Waargenomen kaal = speel(UITSLUITING, "case4-kaal.json", tak);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten [M] 1"), kaal.spoor(), tak::name);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten GEVUURD", "defaults K:Jutter.uitgesloten NIET_GERAAKT"),
                    kaal.dekking(), tak::name);
        }
        // the report plays the exec-model branch and fires through the same entry
        for (String casus : List.of("case1-geweigerd.json", "case4-kaal.json")) {
            ScenarioResultaat r = rapport(UITSLUITING, casus);
            Waargenomen w = speel(UITSLUITING, casus, DroolsBackend.Branch.EXEC_MODEL);
            assertEquals(w.spoor(), r.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " " + v.aantal()).toList(), casus);
            assertEquals(w.dekking(), r.dekking().stream().map(d -> d.regel() + " " + d.soort()).toList(), casus);
        }
    }

    /** The trace gives the versions of one rule one row, the forms keep them apart, and so does the coverage: in 2025
     *  the first version of "jutheffing naar jaartarief" holds and the second does not. */
    @Test void de_vormen_noemen_de_versie_die_vuurde() throws Exception {
        Path jutheffing = GOLDEN.resolve("anchor").resolve("jutterswet-jutheffing");
        Waargenomen w = speel(jutheffing, "case1-tarief-2025.json", DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(List.of("registratiegetal bij aangifte [A1] 1", "jutheffing naar jaartarief [A1] 1"), w.spoor(),
                "de versies van één regel delen een rij");
        assertEquals(Set.of("registratiegetal bij aangifte", "jutheffing naar jaartarief #1"), w.vormen(),
                "de vormen dragen de versie die vuurde, en niet de andere");
        assertEquals(List.of("jutheffing naar jaartarief #1 GEVUURD", "jutheffing naar jaartarief #2 NIET_GERAAKT",
                "registratiegetal bij aangifte GEVUURD"), w.dekking());
    }

    /** A row names each label once, however often its rules bound it: on golden jutterswet-premiestaffel the defaults
     *  of the premie fire 20 times over 14 jutters and vondsten. */
    @Test void een_label_staat_een_keer_in_zijn_rij() throws Exception {
        Path premiestaffel = GOLDEN.resolve("jutterswet-premiestaffel");
        JsonNode c = TmsDocumenten.JSON.readTree(premiestaffel.resolve("case1-staffel.json").toFile());
        List<ScenarioResultaat.Vuring> vuringen = CaseRunner.voorMeting(premiestaffel.resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL).metSessie(c, (bouw, sessie, labels) -> {
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelVan(labels));
                    assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
                    return w.vuringen();
                });
        ScenarioResultaat.Vuring premie = vuringen.stream().filter(v -> v.regel().equals("defaults A:Jutter.premie"))
                .findFirst().orElseThrow(() -> new AssertionError("geen rij voor de premie: " + vuringen));
        assertEquals(20, premie.aantal());
        assertEquals(List.of("J1", "J2", "J3", "J4", "J5", "V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8", "V9"),
                premie.labels().stream().sorted().toList(), "elk label één keer, in de volgorde waarin het gebonden werd");
    }

    /** A model with an object creation fires in generations, with a listener of the driver's own beside the
     *  waarnemer's; the waarnemer hears every phase, the creation rule among them. */
    @Test void een_creatie_die_in_generaties_vuurt_telt_mee() throws Exception {
        Waargenomen w = speel(WRAKBOEKING, "case.json", DroolsBackend.Branch.EXEC_MODEL);
        DroolsBackend.Build bouw = DroolsBackend.build(WRAKBOEKING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        List<String> creatievormen = bouw.execModelTranspiler().creatieregels().stream()
                .map(r -> bouw.execModelTranspiler().vormVan(r)).distinct().sorted().toList();
        assertEquals(List.of("boek een strandrecht bij elk wrakrecht"), creatievormen, "het document heeft één creatievorm");
        assertEquals(List.of("bereken de bergingsvergoeding van een wrakrecht [W1, W2] 2",
                "boek een strandrecht bij elk wrakrecht [W1, W2] 2"), w.spoor());
        assertEquals(List.of("bereken de bergingsvergoeding van een wrakrecht GEVUURD",
                "boek een strandrecht bij elk wrakrecht GEVUURD"), w.dekking());
    }

    // ---------------- two sessions at once ----------------

    private static final int RONDES = 20;

    /** A Jutter of {@code bouw}'s bean model, with the characteristic "vergunning geweigerd" where {@code geweigerd}. */
    private static Object jutter(DroolsBackend.Build bouw, boolean geweigerd) throws Exception {
        Object bean = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident("Jutter", true), true, bouw.beanLoader())
                .getConstructor().newInstance();
        if (geweigerd) bean.getClass().getMethod(Namen.setter("vergunning geweigerd"), boolean.class).invoke(bean, true);
        return bean;
    }

    /** One session of {@code bouw} with one Jutter named {@code label}, put in through {@code voegIn} and fired through
     *  the entry with a waarnemer of its own, after {@code start} where one is given: the two threads of a round fire
     *  together. */
    private static Waargenomen eenSessie(DroolsBackend.Build bouw, boolean geweigerd, String label, CyclicBarrier start)
            throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bouw.beanLoader());
        KieSession sessie = bouw.kieBase().newKieSession();
        try {
            Object jutter = jutter(bouw, geweigerd);
            DroolsBackend.voegIn(bouw, sessie, new Invoer().bean(jutter));
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(o -> o == jutter ? label : null);
            if (start != null) start.await(30, TimeUnit.SECONDS);
            assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
            return waargenomen(bouw, w);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Two sessions of ONE build, as a decision service holds them: A with a refused licence fires the defaults group,
     *  B without one fires the rule. Fired at once on two threads, round after round, each waarnemer gives exactly what
     *  its session gives fired alone, and nothing of the other: no row, no form, no label. */
    @Test void twee_sessies_zien_elkaars_vuringen_niet() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        Waargenomen alleenA = eenSessie(bouw, true, "A", null);
        Waargenomen alleenB = eenSessie(bouw, false, "B", null);
        assertEquals(List.of("defaults K:Jutter.uitgesloten [A] 2"), alleenA.spoor());
        assertEquals(List.of("penninggerechtigd tenzij uitgesloten [B] 1"), alleenB.spoor());
        ExecutorService draden = Executors.newFixedThreadPool(2);
        try {
            for (int ronde = 0; ronde < RONDES; ronde++) {
                CyclicBarrier start = new CyclicBarrier(2);
                Future<Waargenomen> a = draden.submit(() -> eenSessie(bouw, true, "A", start));
                Future<Waargenomen> b = draden.submit(() -> eenSessie(bouw, false, "B", start));
                assertEquals(alleenA, a.get(60, TimeUnit.SECONDS), "ronde " + ronde + ": sessie A");
                assertEquals(alleenB, b.get(60, TimeUnit.SECONDS), "ronde " + ronde + ": sessie B");
            }
        } finally {
            draden.shutdownNow();
        }
    }

    // ---------------- the listener is on during vuur alone ----------------

    private static Set<AgendaEventListener> luisteraars(KieSession sessie) {
        Set<AgendaEventListener> uit = Collections.newSetFromMap(new IdentityHashMap<>());
        uit.addAll(sessie.getAgendaEventListeners());
        return uit;
    }

    /** After {@code vuur} the session has the listeners it had before it, and a firing outside {@code vuur}, a plain
     *  {@code fireAllRules} after an update, adds nothing to the waarnemer. */
    @Test void de_waarnemer_hoort_alleen_het_vuren_van_vuur() throws Exception {
        JsonNode kaal = TmsDocumenten.JSON.readTree(UITSLUITING.resolve("case4-kaal.json").toFile());
        CaseRunner.voorMeting(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).metSessie(kaal,
                (bouw, sessie, labels) -> {
                    Set<AgendaEventListener> voor = luisteraars(sessie);
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelVan(labels));
                    assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
                    assertEquals(voor, luisteraars(sessie), "vuur neemt de luisteraar van de waarnemer weer van de sessie");
                    Waargenomen na = waargenomen(bouw, w);
                    assertEquals(List.of("penninggerechtigd tenzij uitgesloten [M] 1"), na.spoor());
                    Object m = labels.get("M");
                    try {
                        m.getClass().getMethod(Namen.setter("vergunning geweigerd"), boolean.class).invoke(m, true);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    sessie.update(sessie.getFactHandle(m), m);
                    int buiten = sessie.fireAllRules();
                    assertTrue(buiten > 0, "de kale fireAllRules vuurt de defaultsgroep op M");
                    assertEquals(na, waargenomen(bouw, w), "een vuring buiten vuur telt niet mee");
                    return null;
                });
    }

    /** A {@code vuur} that refuses the session takes the listener off as well: a model with a creation fires once. */
    @Test void een_geweigerde_vuur_laat_geen_luisteraar_achter() throws Exception {
        JsonNode c = TmsDocumenten.JSON.readTree(WRAKBOEKING.resolve("case.json").toFile());
        CaseRunner.voorMeting(WRAKBOEKING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).metSessie(c,
                (bouw, sessie, labels) -> {
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelVan(labels));
                    assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
                    Waargenomen na = waargenomen(bouw, w);
                    Set<AgendaEventListener> voor = luisteraars(sessie);
                    IllegalStateException e = assertThrows(IllegalStateException.class,
                            () -> DroolsBackend.vuur(bouw, sessie, w));
                    assertTrue(e.getMessage().startsWith("dit model heeft creatieregels"), e::getMessage);
                    assertEquals(voor, luisteraars(sessie), "ook een weigering neemt de luisteraar weer van de sessie");
                    assertEquals(na, waargenomen(bouw, w));
                    return null;
                });
    }

    // ---------------- the refusals ----------------

    @Test void een_waarnemer_van_een_andere_sessie_wordt_geweigerd() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bouw.beanLoader());
        KieSession eerste = bouw.kieBase().newKieSession();
        KieSession tweede = bouw.kieBase().newKieSession();
        try {
            Object a = jutter(bouw, true), b = jutter(bouw, false);
            DroolsBackend.voegIn(bouw, eerste, new Invoer().bean(a));
            DroolsBackend.voegIn(bouw, tweede, new Invoer().bean(b));
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(o -> o == a ? "A" : o == b ? "B" : null);
            assertNull(DroolsBackend.vuur(bouw, eerste, w).geenFixpunt());
            Set<AgendaEventListener> voor = luisteraars(tweede);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> DroolsBackend.vuur(bouw, tweede, w));
            assertEquals("deze waarnemer nam al een andere sessie waar: een waarnemer hoort bij één sessie, dus geef elke"
                    + " sessie een eigen waarnemer", e.getMessage());
            assertEquals(voor, luisteraars(tweede));
            assertEquals(List.of("defaults K:Jutter.uitgesloten [A] 2"), waargenomen(bouw, w).spoor(),
                    "de weigering laat de waarneming van de eerste sessie zoals ze was");
            // the second session was not fired by the refused call: with a waarnemer of its own it fires as usual
            DroolsBackend.Waarnemer eigen = new DroolsBackend.Waarnemer(o -> o == b ? "B" : null);
            assertNull(DroolsBackend.vuur(bouw, tweede, eigen).geenFixpunt());
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten [B] 1"), waargenomen(bouw, eigen).spoor());
        } finally {
            eerste.dispose();
            tweede.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    @Test void een_vorm_buiten_het_manifest_wordt_geweigerd() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(UITSLUITING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DroolsBackend.dekking(bouw, Set.of("jutheffing naar jaartarief #1")));
        assertEquals("de vorm 'jutheffing naar jaartarief #1' is geen post van het manifest van deze build, dus geen"
                + " sessie van deze build vuurt haar; komen de gevuurde vormen van een andere build?", e.getMessage());
        assertFalse(DroolsBackend.dekking(bouw, Set.of()).isEmpty(), "zonder gevuurde vorm is elke post NIET_GERAAKT");
        assertTrue(DroolsBackend.dekking(bouw, Set.of()).stream()
                .allMatch(d -> d.soort() == ScenarioResultaat.DekkingSoort.NIET_GERAAKT));
    }
}
