package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Fault;
import dev.uchar.regelspraak.transpiler.tms.WritersConflict;

import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entry for a consumer of the exec-model branch (card tms-omschakeling, point 12): a model with a creation
 * fires through {@link DroolsBackend#fire} in generations, {@link DroolsBackend#read} gives what the creations made, and
 * {@link DroolsBackend#setDerivedValues} puts the links they laid on the reference fields of the beans, so that a
 * consumer that reads the beans reaches a made object as it reaches an object of the input.
 *
 * <ul>
 *   <li>Golden {@code anchor/jutterswet-rekenkamer}, case 1: the object creation "richt de rekenkamer op" makes a
 *       Rekenkamer R for J1 over the 1:1 relation taxatieopdracht, and J1's heffing is 70. Before the write-back the
 *       session holds R, its link and the heffing, and J1's bean holds neither; after it J1's reference field
 *       rekenkamer holds R, R's field opdrachtgever holds J1, and J1's heffing is 70.</li>
 *   <li>Golden {@code jutterswet-dubbelverbinding}, case 1: the fact creation "winterberging door de haven" links every
 *       ligger of a veilige haven over winterberging. After the write-back S1's field winterhaven holds H1, the field of
 *       its own fact type, while thuishaven, the relation the creation joined over, still holds what the case gave, and
 *       S4, whose H3 is not veilig, has none: the row {@code RepeatedTypePairWitnessTest} plays on the exec-model
 *       branch, "the creation wrote the winterberging leg".</li>
 *   <li>A session of that model fired with a plain {@code fireAllRules} holds no creation: {@code read} and
 *       {@code setDerivedValues} refuse it, and the sentence names the creations.</li>
 *   <li>So on every model of the branch (the fix round of step 3, point 4): on the values document of
 *       {@link TmsRepeatedFireWitnessTest} both refuse a session that did not fire, or fired with a plain
 *       {@code fireAllRules}, and write nothing; both refuse a session whose last {@code fire} stopped at the cap, with
 *       the sentence of that {@code fire}, and read the next {@code fire} that reaches the fixpoint; and {@code read}
 *       refuses a session whose last {@code fire} broke off with an exception, here the filter document of
 *       {@link TmsFilterFaultWitnessTest} with its beans inserted without {@code insert}, so that the filter fault rule
 *       finds no place. What {@code read} gives is thus always a fixpoint.</li>
 *   <li>And after a {@code fire} that reached its fixpoint (the second fix round of step 3, point 1), both refuse a
 *       session in which something happened outside {@code fire}, naming what happened and the way on, and write
 *       nothing. On {@code anchor/jutterswet-rekenkamer}, case 1, J1's heffing is 70 at the fixpoint; J1's grondslag
 *       becomes 150 ({@code update}) and a plain {@code fireAllRules} follows, which leaves the made Rekenkamer on the
 *       grondslag it was initialised with; the sentence names the update and the first rule the plain fire fired, and
 *       sends the consumer to a fresh session, since {@code fire} refuses a second fire on a model with a creation; a
 *       fresh session of grondslag 150 gives 120 ({@link TmsRepeatedFireWitnessTest}, measured in the reference
 *       interpreter and the ASP runner). On the values document, without a creation, both refuse after an update
 *       alone, with the sentence that asks a {@code fire} first; after that {@code fire} the reading is that of a fresh
 *       session of the changed input. A plain fire between two fires makes {@code fire} refuse as well, so there the
 *       sentence sends the consumer to a fresh session ({@link TmsRepeatedFireWitnessTest}, the row of the plain fire;
 *       until the addition to step 5a1 of 2026-10-10 this row fired again after the plain fire and read the fresh
 *       reading). After the write-back, an update makes
 *       both refuse with the way to a fresh session, since {@code fire} refuses a written-back session. <b>Red before
 *       the change</b> (measured 2026-10-10 on {@code 32a649c4}, the tip of the first fix round): each of the three rows
 *       failed on "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown."; their old form, which
 *       read where these rows expect a refusal, gave the state of the first input: after the update and a plain fire of
 *       two firings, J1's heffing 70, which the write-back then wrote on the bean, where a fresh session gives 120; and
 *       after an update of V1 without a further {@code fire}, the first reading, J1's totaalgewicht 14 and V1 zwaar with
 *       bonus 7, heffing 40, uitkering 20 and vindloon 15.</li>
 *   <li>{@code read} with labels (the fix round of step 3, point 5): on the red case
 *       {@code corpus/red/jutterswet-dubbele-melding}, case 1, two writers give V's meldingsdatum two dates, and the
 *       fault of the fixpoint names V as "V, een Vondst", where {@code read} without labels names it by type and
 *       identity; the write-back with the same labels throws the fault in the same sentence. The reference interpreter
 *       names V alike, "V, a Vondst", and the ASP runner gives "V" beside the fault (measured 2026-10-10; gate 8 pins
 *       both). <b>Red before the change</b> (measured 2026-10-10 on {@code f4b83d7b}): the row does not compile there,
 *       "method lees in class dev.uchar.regelspraak.cases.DroolsBackend cannot be applied to given types", since the
 *       entry read without labels only.</li>
 *   <li>On the exec-model and the DRL-text branch {@code read} refuses: their rules write the beans as they fire.</li>
 *   <li>The entry is public, the seams of the scale harness and the witnesses are not.</li>
 * </ul>
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the first two rows, in which a
 * consumer fired through the package-private driver and wrote back): "J1's field rekenkamer holds the made Rekenkamer
 * ==&gt; expected: not &lt;null&gt;" and "the creation wrote the winterberging leg ==&gt; expected:
 * &lt;dev.uchar.regelspraak.facts.Haven@35267fd4&gt; but was: &lt;null&gt;". The witness itself does not compile
 * there. The rows of point 4 (measured 2026-10-10 on {@code f4b83d7b}, where only a model with a creation was refused,
 * by the {@code CreationPhase} the driver leaves): each of the three new rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown.", the reading handing out what stood, and the
 * row of the plain fire on "expected: &lt;geen fixpunt om te lezen of terug te zetten: deze sessie is niet via
 * DroolsBackend.fire gevuurd, …&gt; but was: &lt;dit model heeft creatieregels en vuurt in generaties
 * (DroolsBackend.fire); de sessie is zonder creatiefase gevuurd, dus zonder de creaties&gt;".
 */
class TmsConsumerWitnessTest {

    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    @Test void an_object_creation_stands_after_the_write_back_on_both_sides_on_the_beans() throws Exception {
        play("anchor/jutterswet-rekenkamer", "case1-berekend.json", (build, session, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "the fixpoint");
            Fixpoint f = DroolsBackend.fixpointOf(build, session);
            assertEquals(1, f.objects().size(), () -> "one made object: " + f.objects());
            Fixpoint.MadeObject r = f.objects().get(0);
            assertEquals("richt de rekenkamer op", r.creation());
            assertSame(j1, r.onderwerp());
            assertEquals("Rekenkamer", r.object().getClass().getSimpleName());
            assertEquals(List.of(new Fixpoint.MadeLink("taxatieopdracht", "opdrachtgever", j1, "rekenkamer",
                    r.object())), f.links());
            assertTrue(f.waarden().stream().anyMatch(w -> w.object() == j1 && w.attribute().equals("heffing")
                    && w.value() instanceof BigDecimal d && d.compareTo(new BigDecimal("70")) == 0),
                    () -> "J1 heffing 70: " + f.waarden());
            assertNull(RegelSpraakRuntime.get(j1, "rekenkamer"), "before the write-back J1's bean holds the input alone");
            assertNull(RegelSpraakRuntime.get(j1, "heffing"), "before the write-back J1's bean holds the input alone");
            DroolsBackend.setDerivedValues(build, session);
            assertSame(r.object(), RegelSpraakRuntime.get(j1, "rekenkamer"), "J1's field rekenkamer holds the made Rekenkamer");
            assertSame(j1, RegelSpraakRuntime.get(r.object(), "opdrachtgever"), "R's field opdrachtgever holds J1");
            assertEquals(0, new BigDecimal("70").compareTo((BigDecimal) RegelSpraakRuntime.get(j1, "heffing")), "J1 heffing");
            // the write-back set a reference field of a bean of the input, its own work, which the watch does not count
            // as a changed relation
            assertEquals(f.links(), DroolsBackend.fixpointOf(build, session).links(),
                    "the reading after the write-back");
            return null;
        });
    }

    @Test void a_fact_creation_stands_after_the_write_back_on_the_field_of_its_own_fact_type() throws Exception {
        play("jutterswet-dubbelverbinding", "case1-binnen-venster.json", (build, session, labels) -> {
            Object h1 = labels.get("H1"), s1 = labels.get("S1"), s4 = labels.get("S4");
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "the fixpoint");
            Fixpoint f = DroolsBackend.fixpointOf(build, session);
            assertTrue(f.links().contains(new Fixpoint.MadeLink("winterberging", "winterhaven", h1,
                    "winterschuit", s1)), () -> "the link of S1 to H1 over winterberging: " + f.links());
            assertNull(RegelSpraakRuntime.get(s1, "winterhaven"), "before the write-back S1's bean holds the input alone");
            DroolsBackend.setDerivedValues(build, session);
            assertSame(h1, RegelSpraakRuntime.get(s1, "winterhaven"), "the creation wrote the winterberging leg");
            assertSame(h1, RegelSpraakRuntime.get(s1, "thuishaven"),
                    "and the joined-over thuisligging leg still says what the case said");
            assertNull(RegelSpraakRuntime.get(s4, "winterhaven"), "H3 is not veilig, so its ligger got nothing");
            assertEquals(f.links(), DroolsBackend.fixpointOf(build, session).links(),
                    "the reading after the write-back");
            return null;
        });
    }

    @Test void a_model_with_creation_fires_via_fire_and_not_with_a_bare_fire() throws Exception {
        play("jutterswet-dubbelverbinding", "case1-binnen-venster.json", (build, session, labels) -> {
            session.fireAllRules();
            String sentence = "geen fixpunt om te lezen of terug te zetten: deze sessie is niet via DroolsBackend.vuur gevuurd,"
                    + " dat tot het fixpunt vuurt en de creaties van dit model in generaties laat vuren";
            assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(build, session))
                    .getMessage());
            assertEquals(sentence, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.setDerivedValues(build, session)).getMessage());
            return null;
        });
    }

    @Test void read_and_write_back_refuse_a_session_that_did_not_fire_via_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Map<String, Object> beans = TmsRepeatedFireWitnessTest.valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                String sentence = "geen fixpunt om te lezen of terug te zetten: deze sessie is niet via DroolsBackend.vuur"
                        + " gevuurd, dat tot het fixpunt vuurt";
                assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session))
                        .getMessage(), "a session that did not fire");
                session.fireAllRules();
                assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session))
                        .getMessage(), "a session fired with a plain fireAllRules");
                assertEquals(sentence, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, session)).getMessage(), "the write-back");
                assertNull(TmsDocuments.read(beans.get("V1"), "uitkering"), "the write-back wrote nothing");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void read_and_write_back_refuse_a_session_whose_last_fire_reached_no_fixpoint() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Map<String, Object> beans = TmsRepeatedFireWitnessTest.valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                DroolsBackend.Firing truncated = DroolsBackend.fire(b, session, 0.01, 1);
                assertNotNull(truncated.noFixpoint(), "the run stops at a cap of a few firings");
                String sentence = "geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur van deze sessie"
                        + " bereikte er geen — " + truncated.noFixpoint();
                assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session))
                        .getMessage(), "the reading");
                assertEquals(sentence, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, session)).getMessage(), "the write-back");
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "a further fire under the public cap");
                assertEquals(TmsRepeatedFireWitnessTest.FIRST_VALUES,
                        TmsRepeatedFireWitnessTest.describe(DroolsBackend.fixpointOf(b, session), beans),
                        "the reading of the last fire, which reached the fixpoint");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void read_refuses_a_session_whose_last_fire_aborted() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterFaultWitnessTest.document(TmsFilterFaultWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            // the gewicht 3, whose root the filter cannot take (until the card zero-division-channel 9 divided by 0)
            Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "3"), List.of());
            TmsRepeatedFireWitnessTest.setRole(v1, "aanmelder", j1);
            KieSession session = b.kieBase().newKieSession();
            try {
                // inserted without insert, so the member has no place, and the filter fault rule stops the fire
                session.insert(j1);
                session.insert(v1);
                RuntimeException e = assertThrows(RuntimeException.class, () -> DroolsBackend.fire(b, session));
                String sentence = assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage();
                assertTrue(sentence.startsWith("geen fixpunt om te lezen of terug te zetten: de laatste DroolsBackend.vuur van"
                        + " deze sessie bereikte er geen — de fire brak af met " + e.getClass().getSimpleName() + ": "), sentence);
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_change_and_a_bare_fire_after_fire_refuse_read_and_write_back_on_a_model_with_creation()
            throws Exception {
        play("anchor/jutterswet-rekenkamer", "case1-berekend.json", (build, session, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "the fixpoint");
            assertEquals("70", heffing(DroolsBackend.fixpointOf(build, session), j1), "J1 heffing at the fixpoint");
            RegelSpraakRuntime.set(j1, "grondslag", new BigDecimal("150"));
            session.update(session.getFactHandle(j1), j1);
            String regel = bareFire(session);
            String sentence = outsideFire(naam(j1) + " werd gewijzigd met update"
                    + (regel == null ? "" : " en de regel \"" + regel + "\" vuurde"), FRESH_AT_CREATION);
            assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(build, session))
                    .getMessage(), "the reading");
            assertEquals(sentence, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.setDerivedValues(build, session)).getMessage(), "the write-back");
            assertNull(RegelSpraakRuntime.get(j1, "heffing"), "the write-back wrote nothing");
            return null;
        });
    }

    @Test void after_an_update_read_and_write_back_read_only_after_a_next_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Map<String, Object> beans = TmsRepeatedFireWitnessTest.valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the first fire");
                Object v1 = beans.get("V1");
                TmsRepeatedFireWitnessTest.set(v1, "gewicht", "3");
                session.update(session.getFactHandle(v1), v1);
                String changed = naam(v1) + " werd gewijzigd met update";
                assertEquals(outsideFire(changed, FIRE_FIRST), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fixpointOf(b, session)).getMessage(), "the reading after the update");
                assertEquals(outsideFire(changed, FIRE_FIRST), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, session)).getMessage(), "the write-back after the update");
                assertNull(TmsDocuments.read(v1, "uitkering"), "the write-back wrote nothing");
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fire the sentence asks for");
                assertEquals(TmsRepeatedFireWitnessTest.SECOND_VALUES,
                        TmsRepeatedFireWitnessTest.describe(DroolsBackend.fixpointOf(b, session), beans),
                        "the reading after that fire, that of a fresh session of the changed input");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void an_update_after_the_write_back_points_read_to_a_fresh_session() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Map<String, Object> beans = TmsRepeatedFireWitnessTest.valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "the fire");
                DroolsBackend.setDerivedValues(b, session);
                assertEquals(TmsRepeatedFireWitnessTest.FIRST_VALUES,
                        TmsRepeatedFireWitnessTest.describe(DroolsBackend.fixpointOf(b, session), beans),
                        "the write-back is the entry's own: the session reads after it as before it");
                Object v1 = beans.get("V1");
                TmsRepeatedFireWitnessTest.set(v1, "gewicht", "3");
                session.update(session.getFactHandle(v1), v1);
                assertEquals(outsideFire(naam(v1) + " werd gewijzigd met update", FRESH_AFTER_WRITE_BACK),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(),
                        "the reading after an update of a written-back session");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void read_with_labels_names_the_object_of_a_fault_at_its_label() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "red", "jutterswet-dubbele-melding", "ir.json");
        JsonNode c = TmsDocuments.JSON.readTree(Files.readString(ir.resolveSibling("case1-beide-schrijvers-gelden.json")));
        CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL).withSession(c, (build, session, labels) -> {
            Map<Object, String> toLabel = new IdentityHashMap<>();
            labels.forEach((l, beanObject) -> toLabel.put(beanObject, l));
            Object v = labels.get("V");
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "the fixpoint, at which the fault stands");
            String sentence = "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"melding op"
                    + " vinddag\" schrijft 2026-03-04 en de regel \"melding op aanspoeldag\" schrijft 2026-03-01 in het"
                    + " attribuut meldingsdatum van %s; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
            List<Fault> labelled = DroolsBackend.fixpointOf(build, session, toLabel::get).faults();
            assertEquals(1, labelled.size(), () -> "one fault: " + labelled);
            assertEquals(String.format(sentence, "V, een Vondst"), labelled.get(0).message(), "named by its label");
            assertEquals(String.format(sentence, "een Vondst (" + v.getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(v)) + ")"),
                    DroolsBackend.fixpointOf(build, session).faults().get(0).message(), "without labels, by type and identity");
            assertEquals(labelled.get(0).message(), assertThrows(WritersConflict.class,
                    () -> DroolsBackend.setDerivedValues(build, session, toLabel::get)).getMessage(),
                    "the fault the write-back throws, named alike");
            return null;
        });
    }

    @Test void the_entry_is_public_and_the_seams_are_not() throws Exception {
        Class<?> d = DroolsBackend.class;
        for (Method m : List.of(
                d.getDeclaredMethod("insert", DroolsBackend.Build.class, KieSession.class, Input.class),
                d.getDeclaredMethod("fire", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("fixpointOf", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("fixpointOf", DroolsBackend.Build.class, KieSession.class, Function.class),
                d.getDeclaredMethod("setDerivedValues", DroolsBackend.Build.class, KieSession.class),
                d.getDeclaredMethod("setDerivedValues", DroolsBackend.Build.class, KieSession.class, Function.class)))
            assertTrue(Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()), m.toString());
        for (Class<?> c : List.of(DroolsBackend.Firing.class, Input.class, Fixpoint.class))
            assertTrue(Modifier.isPublic(c.getModifiers()), c.toString());
        for (Method m : List.of(
                d.getDeclaredMethod("fire", DroolsBackend.Build.class, KieSession.class, int.class),
                d.getDeclaredMethod("fire", DroolsBackend.Build.class, KieSession.class, double.class, int.class)))
            assertFalse(Modifier.isPublic(m.getModifiers()), m.toString());
    }

    // ---------------- helpers ----------------

    /** Sets up case {@code caseFileName} of golden {@code golden} on the exec-model branch as the replay does, through
     *  {@link DroolsBackend#insert}, and hands the session, not yet fired, to {@code measurement}. */
    private static void play(String golden, String caseFileName, CaseRunner.SessionMeasurement<Void> measurement) throws Exception {
        Path ir = CORPUS.resolve(golden).resolve("ir.json");
        JsonNode c = TmsDocuments.JSON.readTree(Files.readString(ir.resolveSibling(caseFileName)));
        CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL).withSession(c, measurement);
    }

    /** The way on where something happened outside {@code fire} after it: a further {@code fire}, or a fresh session
     *  where {@code fire} refuses the session. */
    static final String FIRE_FIRST = "vuur haar eerst opnieuw met DroolsBackend.vuur";
    static final String FRESH_AT_CREATION = "dit model heeft creatieregels, dus DroolsBackend.vuur weigert een tweede"
            + " fire: open een verse sessie";
    static final String FRESH_AFTER_WRITE_BACK = "deze sessie is teruggezet, dus DroolsBackend.vuur weigert haar: open een"
            + " verse sessie met beans die alleen invoer dragen";

    /** The sentence of {@code read} and {@code setDerivedValues} where {@code description} happened outside {@code fire}
     *  after the last one, with {@code way} the way on. */
    static String outsideFire(String description, String way) {
        return "geen fixpunt om te lezen of terug te zetten: sinds de laatste DroolsBackend.vuur van deze sessie gebeurde"
                + " er iets buiten vuur om, wat het fixpunt van die vuur kan veranderen (" + description + "); " + way;
    }

    /** Fires {@code session} with a plain {@code fireAllRules}, as a consumer can, and gives the name of the first rule
     *  that fired, or null. */
    static String bareFire(KieSession session) {
        String[] first = {null};
        AgendaEventListener l = new DefaultAgendaEventListener() {
            @Override public void beforeMatchFired(BeforeMatchFiredEvent e) {
                if (first[0] == null) first[0] = e.getMatch().getRule().getName();
            }
        };
        session.addEventListener(l);
        try {
            session.fireAllRules();
        } finally {
            session.removeEventListener(l);
        }
        return first[0];
    }

    /** An object as the sentences of the entry name it: its type and its identity. */
    static String naam(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    /** The heffing {@code f} derives for {@code jutter}, as a plain number, or null. */
    private static String heffing(Fixpoint f, Object jutter) {
        for (Fixpoint.DerivedValue w : f.waarden())
            if (w.object() == jutter && w.attribute().equals("heffing"))
                return ((BigDecimal) w.value()).stripTrailingZeros().toPlainString();
        return null;
    }
}
