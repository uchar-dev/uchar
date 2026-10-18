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
 * pinned in {@code ScenarioReportWitnessTest}) without a copy of the report's listener. Every session here is set up as
 * a consumer sets it up, through {@code CaseRunner.withSession} or with its own beans and {@code DroolsBackend.voegIn},
 * and fired through the entry alone.
 *
 * <p>The rows pin what the report's rows cannot see: that two sessions of one build, fired at once on two threads,
 * each with a waarnemer of its own, see only their own firings; that the waarnemer's listener is on the session during
 * {@code vuur} alone, so a firing outside it is not counted, also after a {@code vuur} that refuses the session; and the
 * two refusals, of a waarnemer that observed another session and of a fired form outside the manifest.
 */
class ObserverWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");
    private static final Path EXCLUSION = GOLDEN.resolve("jutterswet-uitsluiting-rgs");
    /** An object creation: the session fires in generations ({@code DroolsBackend.vuur(Build, KieSession, int)}). */
    private static final Path WRAKBOEKING = GOLDEN.resolve("jutterswet-wrakboeking");

    /** What a waarnemer recorded, rendered for comparison: per row "form [labels] count", the exact forms, and per
     *  manifest entry "name SOORT". */
    private record Observed(List<String> trace, Set<String> vormen, List<String> dekking) {}

    private static Observed observed(DroolsBackend.Build bouw, DroolsBackend.Waarnemer w) {
        List<String> trace = new ArrayList<>();
        for (ScenarioResultaat.Vuring v : w.vuringen()) trace.add(v.regel() + " " + v.labels() + " " + v.aantal());
        List<String> dekking = new ArrayList<>();
        for (ScenarioResultaat.DekkingRegel d : DroolsBackend.dekking(bouw, w.vormen())) dekking.add(d.regel() + " " + d.soort());
        return new Observed(trace, w.vormen(), dekking);
    }

    /** The label of an input bean by identity in {@code labels}, null for any other object. */
    private static java.util.function.Function<Object, String> labelOf(Map<String, Object> labels) {
        return o -> {
            for (Map.Entry<String, Object> e : labels.entrySet()) if (e.getValue() == o) return e.getKey();
            return null;
        };
    }

    /** Case {@code goldenCase} of {@code golden} on {@code branch}, set up as the replay sets it up and fired through the entry
     *  with a waarnemer over the case labels. */
    private static Observed play(Path golden, String goldenCase, DroolsBackend.Branch branch) throws Exception {
        JsonNode c = TmsDocuments.JSON.readTree(golden.resolve(goldenCase).toFile());
        return CaseRunner.forMeasurement(golden.resolve("ir.json"), branch).withSession(c, (bouw, sessie, labels) -> {
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelOf(labels));
            assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt(), branch + ": the fire reaches its fixpoint");
            return observed(bouw, w);
        });
    }

    private static ScenarioResultaat rapport(Path golden, String goldenCase) {
        return CaseRunner.rapport(golden.resolve("ir.json"), List.of(golden.resolve(goldenCase))).get(0);
    }

    @Test void the_observer_gives_on_both_branches_what_the_report_gives() throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            Observed refused = play(EXCLUSION, "case1-geweigerd.json", branch);
            assertEquals(List.of("defaults K:Jutter.uitgesloten [M] 2"), refused.trace(),
                    branch + ": de defaultsgroep vuurt twee keer op M; de stand van het kenmerk heeft geen rij");
            assertEquals(Set.of("defaults K:Jutter.uitgesloten"), refused.vormen(), branch::name);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten NIET_GERAAKT", "defaults K:Jutter.uitgesloten GEVUURD"),
                    refused.dekking(), branch + ": één regel per post van het manifest, in zijn volgorde");
            Observed bare = play(EXCLUSION, "case4-kaal.json", branch);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten [M] 1"), bare.trace(), branch::name);
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten GEVUURD", "defaults K:Jutter.uitgesloten NIET_GERAAKT"),
                    bare.dekking(), branch::name);
        }
        // the report plays the exec-model branch and fires through the same entry
        for (String goldenCase : List.of("case1-geweigerd.json", "case4-kaal.json")) {
            ScenarioResultaat r = rapport(EXCLUSION, goldenCase);
            Observed w = play(EXCLUSION, goldenCase, DroolsBackend.Branch.EXEC_MODEL);
            assertEquals(w.trace(), r.trace().vuringen().stream().map(v -> v.regel() + " " + v.labels() + " " + v.aantal()).toList(), goldenCase);
            assertEquals(w.dekking(), r.dekking().stream().map(d -> d.regel() + " " + d.soort()).toList(), goldenCase);
        }
    }

    /** The trace gives the versions of one rule one row, the forms keep them apart, and so does the coverage: in 2025
     *  the first version of "jutheffing naar jaartarief" holds and the second does not. */
    @Test void the_forms_name_the_version_that_fired() throws Exception {
        Path jutheffing = GOLDEN.resolve("anchor").resolve("jutterswet-jutheffing");
        Observed w = play(jutheffing, "case1-tarief-2025.json", DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(List.of("registratiegetal bij aangifte [A1] 1", "jutheffing naar jaartarief [A1] 1"), w.trace(),
                "de versies van één regel delen een rij");
        assertEquals(Set.of("registratiegetal bij aangifte", "jutheffing naar jaartarief #1"), w.vormen(),
                "de vormen dragen de versie die vuurde, en niet de andere");
        assertEquals(List.of("jutheffing naar jaartarief #1 GEVUURD", "jutheffing naar jaartarief #2 NIET_GERAAKT",
                "registratiegetal bij aangifte GEVUURD"), w.dekking());
    }

    /** A row names each label once, however often its rules bound it: on golden jutterswet-premiestaffel the defaults
     *  of the premie fire 20 times over 14 jutters and vondsten. */
    @Test void a_label_stands_one_time_in_its_row() throws Exception {
        Path premiestaffel = GOLDEN.resolve("jutterswet-premiestaffel");
        JsonNode c = TmsDocuments.JSON.readTree(premiestaffel.resolve("case1-staffel.json").toFile());
        List<ScenarioResultaat.Vuring> vuringen = CaseRunner.forMeasurement(premiestaffel.resolve("ir.json"),
                DroolsBackend.Branch.EXEC_MODEL).withSession(c, (bouw, sessie, labels) -> {
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelOf(labels));
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
    @Test void a_creation_that_fires_in_generations_counts_along() throws Exception {
        Observed w = play(WRAKBOEKING, "case.json", DroolsBackend.Branch.EXEC_MODEL);
        DroolsBackend.Build bouw = DroolsBackend.build(WRAKBOEKING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        List<String> creationForms = bouw.execModelTranspiler().creationRules().stream()
                .map(r -> bouw.execModelTranspiler().formOf(r)).distinct().sorted().toList();
        assertEquals(List.of("boek een strandrecht bij elk wrakrecht"), creationForms, "het document heeft één creatievorm");
        assertEquals(List.of("bereken de bergingsvergoeding van een wrakrecht [W1, W2] 2",
                "boek een strandrecht bij elk wrakrecht [W1, W2] 2"), w.trace());
        assertEquals(List.of("bereken de bergingsvergoeding van een wrakrecht GEVUURD",
                "boek een strandrecht bij elk wrakrecht GEVUURD"), w.dekking());
    }

    // ---------------- two sessions at once ----------------

    private static final int ROUNDS = 20;

    /** A Jutter of {@code bouw}'s bean model, with the characteristic "vergunning geweigerd" where {@code geweigerd}. */
    private static Object jutter(DroolsBackend.Build bouw, boolean refused) throws Exception {
        Object bean = Class.forName(FactModelGenerator.PAKKET + "." + Namen.ident("Jutter", true), true, bouw.beanLoader())
                .getConstructor().newInstance();
        if (refused) bean.getClass().getMethod(Namen.setter("vergunning geweigerd"), boolean.class).invoke(bean, true);
        return bean;
    }

    /** One session of {@code bouw} with one Jutter named {@code label}, put in through {@code voegIn} and fired through
     *  the entry with a waarnemer of its own, after {@code start} where one is given: the two threads of a round fire
     *  together. */
    private static Observed oneSession(DroolsBackend.Build bouw, boolean refused, String label, CyclicBarrier start)
            throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bouw.beanLoader());
        KieSession sessie = bouw.kieBase().newKieSession();
        try {
            Object jutter = jutter(bouw, refused);
            DroolsBackend.voegIn(bouw, sessie, new Invoer().bean(jutter));
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(o -> o == jutter ? label : null);
            if (start != null) start.await(30, TimeUnit.SECONDS);
            assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
            return observed(bouw, w);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Two sessions of ONE build, as a decision service holds them: A with a refused licence fires the defaults group,
     *  B without one fires the rule. Fired at once on two threads, round after round, each waarnemer gives exactly what
     *  its session gives fired alone, and nothing of the other: no row, no form, no label. */
    @Test void two_sessions_do_not_see_each_others_firings() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        Observed onlyA = oneSession(bouw, true, "A", null);
        Observed onlyB = oneSession(bouw, false, "B", null);
        assertEquals(List.of("defaults K:Jutter.uitgesloten [A] 2"), onlyA.trace());
        assertEquals(List.of("penninggerechtigd tenzij uitgesloten [B] 1"), onlyB.trace());
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int ronde = 0; ronde < ROUNDS; ronde++) {
                CyclicBarrier start = new CyclicBarrier(2);
                Future<Observed> a = threads.submit(() -> oneSession(bouw, true, "A", start));
                Future<Observed> b = threads.submit(() -> oneSession(bouw, false, "B", start));
                assertEquals(onlyA, a.get(60, TimeUnit.SECONDS), "ronde " + ronde + ": sessie A");
                assertEquals(onlyB, b.get(60, TimeUnit.SECONDS), "ronde " + ronde + ": sessie B");
            }
        } finally {
            threads.shutdownNow();
        }
    }

    // ---------------- the listener is on during vuur alone ----------------

    private static Set<AgendaEventListener> listeners(KieSession sessie) {
        Set<AgendaEventListener> out = Collections.newSetFromMap(new IdentityHashMap<>());
        out.addAll(sessie.getAgendaEventListeners());
        return out;
    }

    /** After {@code vuur} the session has the listeners it had before it, and a firing outside {@code vuur}, a plain
     *  {@code fireAllRules} after an update, adds nothing to the waarnemer. */
    @Test void the_observer_hears_only_the_firing_of_fire() throws Exception {
        JsonNode bare = TmsDocuments.JSON.readTree(EXCLUSION.resolve("case4-kaal.json").toFile());
        CaseRunner.forMeasurement(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).withSession(bare,
                (bouw, sessie, labels) -> {
                    Set<AgendaEventListener> before = listeners(sessie);
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelOf(labels));
                    assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
                    assertEquals(before, listeners(sessie), "vuur neemt de luisteraar van de waarnemer weer van de sessie");
                    Observed after = observed(bouw, w);
                    assertEquals(List.of("penninggerechtigd tenzij uitgesloten [M] 1"), after.trace());
                    Object m = labels.get("M");
                    try {
                        m.getClass().getMethod(Namen.setter("vergunning geweigerd"), boolean.class).invoke(m, true);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                    sessie.update(sessie.getFactHandle(m), m);
                    int outside = sessie.fireAllRules();
                    assertTrue(outside > 0, "de kale fireAllRules vuurt de defaultsgroep op M");
                    assertEquals(after, observed(bouw, w), "een vuring buiten vuur telt niet mee");
                    return null;
                });
    }

    /** A {@code vuur} that refuses the session takes the listener off as well: a model with a creation fires once. */
    @Test void a_refused_fire_leaves_no_listener_behind() throws Exception {
        JsonNode c = TmsDocuments.JSON.readTree(WRAKBOEKING.resolve("case.json").toFile());
        CaseRunner.forMeasurement(WRAKBOEKING.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL).withSession(c,
                (bouw, sessie, labels) -> {
                    DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(labelOf(labels));
                    assertNull(DroolsBackend.vuur(bouw, sessie, w).geenFixpunt());
                    Observed after = observed(bouw, w);
                    Set<AgendaEventListener> before = listeners(sessie);
                    IllegalStateException e = assertThrows(IllegalStateException.class,
                            () -> DroolsBackend.vuur(bouw, sessie, w));
                    assertTrue(e.getMessage().startsWith("dit model heeft creatieregels"), e::getMessage);
                    assertEquals(before, listeners(sessie), "ook een weigering neemt de luisteraar weer van de sessie");
                    assertEquals(after, observed(bouw, w));
                    return null;
                });
    }

    // ---------------- the refusals ----------------

    @Test void an_observer_of_another_session_is_refused() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bouw.beanLoader());
        KieSession eerste = bouw.kieBase().newKieSession();
        KieSession tweede = bouw.kieBase().newKieSession();
        try {
            Object a = jutter(bouw, true), b = jutter(bouw, false);
            DroolsBackend.voegIn(bouw, eerste, new Invoer().bean(a));
            DroolsBackend.voegIn(bouw, tweede, new Invoer().bean(b));
            DroolsBackend.Waarnemer w = new DroolsBackend.Waarnemer(o -> o == a ? "A" : o == b ? "B" : null);
            assertNull(DroolsBackend.vuur(bouw, eerste, w).geenFixpunt());
            Set<AgendaEventListener> before = listeners(tweede);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> DroolsBackend.vuur(bouw, tweede, w));
            assertEquals("deze waarnemer nam al een andere sessie waar: een waarnemer hoort bij één sessie, dus geef elke"
                    + " sessie een eigen waarnemer", e.getMessage());
            assertEquals(before, listeners(tweede));
            assertEquals(List.of("defaults K:Jutter.uitgesloten [A] 2"), observed(bouw, w).trace(),
                    "de weigering laat de waarneming van de eerste sessie zoals ze was");
            // the second session was not fired by the refused call: with a waarnemer of its own it fires as usual
            DroolsBackend.Waarnemer own = new DroolsBackend.Waarnemer(o -> o == b ? "B" : null);
            assertNull(DroolsBackend.vuur(bouw, tweede, own).geenFixpunt());
            assertEquals(List.of("penninggerechtigd tenzij uitgesloten [B] 1"), observed(bouw, own).trace());
        } finally {
            eerste.dispose();
            tweede.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    @Test void a_form_outside_the_manifest_is_refused() throws Exception {
        DroolsBackend.Build bouw = DroolsBackend.build(EXCLUSION.resolve("ir.json"), DroolsBackend.Branch.EXEC_MODEL);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DroolsBackend.dekking(bouw, Set.of("jutheffing naar jaartarief #1")));
        assertEquals("de vorm 'jutheffing naar jaartarief #1' is geen post van het manifest van deze build, dus geen"
                + " sessie van deze build vuurt haar; komen de gevuurde vormen van een andere build?", e.getMessage());
        assertFalse(DroolsBackend.dekking(bouw, Set.of()).isEmpty(), "zonder gevuurde vorm is elke post NIET_GERAAKT");
        assertTrue(DroolsBackend.dekking(bouw, Set.of()).stream()
                .allMatch(d -> d.soort() == ScenarioResultaat.DekkingSoort.NIET_GERAAKT));
    }
}
