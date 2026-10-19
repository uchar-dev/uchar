package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Fault;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.defaultEntry;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two fires in one session of the exec-model branch, through the entry a consumer has ({@link DroolsBackend#insert},
 * {@link DroolsBackend#fire}, {@link DroolsBackend#fixpointOf}), with an input change between them (card tms-omschakeling,
 * points 4 and 11). The beans hold the input alone between the fires, so a derivation whose reads change lapses, a
 * fault arises or lapses, and an initialisation fires again, and the second reading equals that of a fresh session of
 * the new input.
 *
 * <p>The values document: a Jutter J1 with Vondsten V1 (gewicht 10) and V2 (gewicht 4), "De uitkering van een Vondst
 * moet berekend worden als zijn gewicht plus 10", "Een Vondst is zwaar indien zijn gewicht groter is dan 5", "De bonus
 * van een Vondst moet gesteld worden op 7 indien hij zwaar is", "Het totaalgewicht van een Jutter moet berekend worden
 * als de som van de gewichten van zijn vondsten", "Het vindloon van een Vondst moet geïnitialiseerd worden op zijn
 * gewicht plus 5", and a defaults group that gives the heffing 40 where the Vondst is zwaar. At the first fixpoint: V1
 * uitkering 20, zwaar, bonus 7, vindloon 15, heffing 40; V2 uitkering 14, vindloon 9; J1 totaalgewicht 14. Then V1's
 * gewicht becomes 3 ({@code update}): uitkering 13, not zwaar, no bonus and no heffing, vindloon 8, J1 totaalgewicht 7,
 * the values of a fresh session. The faults document: "Het aandeel van een Vondst moet berekend worden als 100 gedeeld
 * door zijn noemer", V1 noemer 0 and V2 noemer 5, then V1 noemer 4 and V2 noemer 0: the fault on V1 lapses and V1 has
 * aandeel 25, and a fault on V2 arises. The verifier passes both documents, and the reference interpreter and the ASP
 * runner give these values on both inputs of the values document, and on the faults document "division by numeric
 * zero" for a Vondst of noemer 0 and aandeel 20 and 25 for noemer 5 and 4, one Vondst per case, since there a fault
 * ends the evaluation (measured 2026-10-10).
 *
 * <p>After the write-back ({@link DroolsBackend#setDerivedValues}) the beans hold derived values beside their input,
 * and {@link DroolsBackend#fire} refuses the session with the sentence that says what to do instead.
 *
 * <p>A session of a model with a creation fires once (the fix round of step 3, point 1). On golden
 * {@code anchor/jutterswet-rekenkamer}, case 1, the object creation makes a Rekenkamer whose grondslag and aftrek it
 * initialises from J1's, 100 and 30, and J1's heffing is 70. Then J1's grondslag becomes 150 ({@code update}), and
 * {@code fire} refuses the session, with the sentence that what a creation made is stated and that the way is a fresh
 * session per input. A fresh session of grondslag 150 gives heffing 120, as the reference interpreter and the ASP runner
 * give it on that case (measured 2026-10-10). <b>Red before the change</b> (measured 2026-10-10 on {@code f4b83d7b}):
 * the second {@code fire} raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was
 * thrown.", and the old form of the row, fire, change, fire and read, gave the state of the first input: "expected:
 * &lt;[…, J1.heffing = 120, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 120]&gt; but was:
 * &lt;[…, J1.heffing = 70, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 70]&gt;", since the
 * made Rekenkamer kept the grondslag it was initialised with.
 *
 * <p>Between two fires the input changes through {@code update} on the beans {@code insert} put in, and in no other
 * way (the second fix round of step 3, point 2). On the values document, after the first fire, V1's vinder becomes a
 * Jutter J2 that the input did not give ({@code update}): {@code fire} refuses, since the update changed a relation
 * (until the addition to step 5a1 it refused here in the sentence of the reference check of {@code insert}, which it
 * now gives where the field changed without an update), and {@code read} still refuses after that refusal, since
 * the update stands. A fresh session that gives J2 derives J2's totaalgewicht 10 and J1's 4. Where J2 is inserted with
 * {@code session.insert} before V1 is set on it, the reference check would pass, as J2 is in the session; the watch
 * refuses first ({@code Watch}): input came in outside {@code insert}, which states the places of a list and the
 * bucket scalars over the beans of one input, so a new object asks a fresh session. The watch refuses alike a bean
 * deleted between two fires, and a bean inserted between {@code insert} and the first fire. The reference interpreter
 * and the ASP runner give the reading with J2 given (measured 2026-10-10; the verifier passes the document). <b>Red
 * before the change</b> (measured 2026-10-10 on {@code 32a649c4}): both rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown."; the old form of the first row fired a second
 * time without a word and read no derivation for J2, "expected: &lt;[J1.totaalgewicht = 4, J2.totaalgewicht = 10, …]&gt;
 * but was: &lt;[J1.totaalgewicht = 4, V1 is zwaar, …]&gt;"; with J2 inserted the old form read what a fresh session
 * reads, and on the filter document of {@link TmsFilterFaultWitnessTest} a Vondst inserted between two fires stopped
 * the second fire with a {@code ConsequenceException} out of the filter fault rule, "… maar een lid van zijn route
 * heeft geen Lijstplaats onder het veld 'aanmelder' …", since only {@code insert} states the places.
 *
 * <p>Outside {@code fire} a consumer changes nothing but an attribute or a characteristic of a bean {@code insert} put
 * in, with {@code update}, from {@code insert} on (the addition to step 5a1, 2026-10-10). {@code fire} refuses, naming
 * what happened and sending the consumer to a fresh session, and {@code read} and {@code setDerivedValues} refuse
 * with the way to a fresh session:
 * <ul>
 *   <li>a global that changed after the first {@code fire}: on golden {@code jutterswet-aanvoergeld} the aanvoertarief
 *       12.50 becomes 22.50 with {@code setGlobal}, which no rule sees, since the branch states a global as a fact once,
 *       at the first fire; a fresh session at 22.50 gives J1, J2 and J3 the aanvoergeld 332.50, 109.90 and 22.50, as the
 *       reference interpreter and the ASP runner give it ("oracle: PASS (7 expectations)", "clingo: PASS (7
 *       expectations)", measured 2026-10-10);</li>
 *   <li>a fact of the branch deleted ({@code Value} of V1's uitkering) or updated, and a fact of no kind {@code insert}
 *       puts in inserted;</li>
 *   <li>a plain {@code fireAllRules}, named by the first rule it fired;</li>
 *   <li>a relation changed, a reference field of a bean of the input, since {@code insert} numbers the places of a list
 *       over the relations of one input. On the filter document of {@link TmsFilterFaultWitnessTest} a Vondst V2 (9 by 0)
 *       without an aanmelder gets J1, whose list holds V1 (10 by 2): at the first fixpoint J1's zware vondsten is 1,
 *       as the reference interpreter gives it, and a fresh session of the new input faults on V2's 9; and a Vondst V1
 *       (9 by 0) moves from J1 to J2, whose list holds V2 (4 by 0), each at place 0 of its list: a fresh session gives
 *       J1's zware vondsten 0 and the fault of J2's first member, V1's 9 (RUNTIME-SEMANTICS ch. 5.11), where the old
 *       session kept both members at place 0 and named V2's 4. The reference interpreter faults on both fresh inputs,
 *       "division by numeric zero", a sentence without the operands, and gives J1's 0 where J2 is not actief (measured
 *       2026-10-10). The ASP runner faults on all of these inputs, also where the fold is not evaluated: it divides in
 *       the filter of every Vondst ("viol("division by numeric zero","V2")" with V2 in no list), a divergence of its own
 *       that these rows do not pin. A reference field set without an update makes no event: set to an object outside
 *       the session it is refused in the sentence of the reference check of {@code fire}, and set to an object of the
 *       input in the sentence of the comparison of the relations (below);</li>
 *   <li>the agenda changed: {@code Agenda.clear} after {@code insert}, before the first fire, of which Drools makes no
 *       event; {@code AgendaGroup.clear} after a fire, which Drools queues for the next fire; and
 *       {@code AgendaGroup.setFocus} on a creation group after a fire on {@code anchor/jutterswet-rekenkamer};</li>
 *   <li>a fired session from which a consumer took the watch: what happened since is not to be seen.</li>
 * </ul>
 * <b>Red before the change</b> (measured 2026-10-10 on {@code 81ac1d6c}): nine rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown.", the relation that comes into being on
 * "Unexpected exception type thrown, expected: &lt;java.lang.IllegalStateException&gt; but was:
 * &lt;org.kie.api.runtime.rule.ConsequenceException&gt;", and the two rows whose sentence changed on the sentence. The
 * old form of each (not committed) fired a second time without a word: at tarief 22.50 it read J1, J2 and J3 at 322.5,
 * 99.9 and 12.5; with V1's uitkering deleted it read no uitkering for V1, where a fresh session gives 20; the new
 * relation stopped the fire on "… heeft geen Lijstplaats onder het veld 'aanmelder' …"; the moved Vondst gave J2 the
 * fault "4 gedeeld door 0"; after {@code Agenda.clear} the fire derived nothing at all, no uitkering 20 for V1 and no
 * totaalgewicht 14 for J1; and after {@code AgendaGroup.clear} the next fire stopped on an
 * {@code UnsupportedOperationException} out of Drools, while {@code read} read without a word before it.
 *
 * <p>A relation changed without an update, and an action of an unknown kind on the propagation list (the lens on step
 * 5a1, 2026-10-10). On the values document with a second Jutter J2 in the input, V1's vinder becomes J2 after the
 * first fire without an update, which makes no event: {@code read}, {@code setDerivedValues} and {@code fire}
 * refuse, naming the field and the two Jutters, since the watch compares the reference fields of the beans of the
 * input with what they held at {@code insert}; a fresh session gives J1 4 and J2 10 ({@link #WITH_J2}). The reference
 * interpreter and the ASP runner give J1 14 and J2 leeg on the first input and J1 4 and J2 10 on the changed one, and
 * the verifier passes the document (measured 2026-10-10). The write-back sets reference fields itself, the links of a
 * creation, and those do not count as a changed relation ({@link TmsConsumerWitnessTest}). An action of a kind the
 * watch does not know, queued with {@code InternalAgenda.addPropagation} as another version of Drools could queue
 * one, makes the three refuse in the sentence that names its class, and the refused fire runs nothing. <b>Red before
 * the change</b> (measured 2026-10-10 on {@code fad6a24c}): both rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown."; the old form (not committed) fired a second
 * time with 0 firings and read J1 totaalgewicht 14 and no value for J2, and with the action queued it read the
 * fixpoint without a word and the second fire ran the action.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the witness, in which a
 * consumer could only fire, write back and read the beans): after the second fire and write-back V1 still carried
 * zwaar, bonus 7, heffing 40 and vindloon 15, "second reading ==&gt; expected: &lt;[J1.totaalgewicht = 7, V1.uitkering =
 * 13, V1.vindloon = 8, V2.uitkering = 14, V2.vindloon = 9]&gt; but was: &lt;[J1.totaalgewicht = 7, V1 is zwaar, V1.bonus
 * = 7, V1.heffing = 40, V1.uitkering = 13, V1.vindloon = 15, V2.uitkering = 14, V2.vindloon = 9]&gt;", and the fire
 * after the write-back raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown." The witness itself does not compile there: the entry had no {@code insert}, no public {@code fire} and no
 * {@code read}.
 */
class TmsRepeatedFireWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("totaalgewicht", WHOLE)), List.of()),
            objectType("Vondst", List.of(attribute("gewicht", WHOLE), attribute("uitkering", WHOLE),
                    attribute("bonus", WHOLE), attribute("vindloon", WHOLE), attribute("heffing", WHOLE),
                    attribute("noemer", WHOLE), attribute("aandeel", TmsFilterFaultWitnessTest.RATIONAL)), List.of("zwaar"))),
            List.of(factTypeOneToMany("vinding", "vinder", "Jutter", "vondst", "Vondst", "vondsten")));

    static final List<String> VALUE_RULES = List.of(
            computingRule("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binary("plus", ownRead("gewicht", "Vondst"), number("10")), "null"),
            characteristicRule("zwaarte", "Vondst", "zwaar", comparison("greater", ownRead("gewicht", "Vondst"), number("5"))),
            computingRule("bonus", "GELIJKSTELLING", "Vondst", "bonus", number("7"), check("zwaar", false)),
            computingRule("totaalgewicht", "BEREKENING", "Jutter", "totaalgewicht",
                    aggregation("sum", ownRead("gewicht", "Vondst"), "Vondst", sourceRole("vinding", "vondst"), "null",
                            "null", WHOLE, "de som van de gewichten van zijn vondsten"), "null"),
            computingRule("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon",
                    binary("plus", ownRead("gewicht", "Vondst"), number("5")), "null"));

    static final List<String> GROUPS = List.of(attributeGroup("heffing van een vondst", "Vondst", "heffing",
            List.of(defaultEntry(check("zwaar", false), number("40")))));

    /** "Het aandeel van een Vondst moet berekend worden als de wortel van zijn noemer", the fault device of
     *  {@link TmsDocuments#root}; until the card zero-division-channel "100 gedeeld door zijn noemer". */
    static final List<String> FAULT_RULES = List.of(computingRule("aandeel", "BEREKENING", "Vondst", "aandeel",
            TmsDocuments.root(ownRead("noemer", "Vondst"), "de wortel van zijn noemer"), "null"));

    static Path valueDocument() throws Exception {
        return TmsDocuments.document(MODEL, VALUE_RULES, List.of(), GROUPS);
    }

    static Path faultDocument() throws Exception {
        return TmsDocuments.document(MODEL, FAULT_RULES);
    }

    static final List<String> FIRST_VALUES = List.of("J1.totaalgewicht = 14", "V1 is zwaar", "V1.bonus = 7",
            "V1.heffing = 40", "V1.uitkering = 20", "V1.vindloon = 15", "V2.uitkering = 14", "V2.vindloon = 9");
    static final List<String> SECOND_VALUES = List.of("J1.totaalgewicht = 7", "V1.uitkering = 13", "V1.vindloon = 8",
            "V2.uitkering = 14", "V2.vindloon = 9");


    @Test void a_derivation_lapses_and_an_initialisation_fires_again_between_two_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                assertEquals(FIRST_VALUES, describe(DroolsBackend.fixpointOf(b, session), beans), "first reading");
                Object v1 = beans.get("V1");
                // between the fires the beans hold the input alone
                assertNull(TmsDocuments.read(v1, "uitkering"), "V1 uitkering on the bean between the fires");
                assertNull(TmsDocuments.read(v1, "vindloon"), "V1 vindloon on the bean between the fires");
                assertFalse(TmsDocuments.has(v1, "zwaar"), "V1 zwaar on the bean between the fires");
                set(v1, "gewicht", "3");
                session.update(session.getFactHandle(v1), v1);
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "second fire");
                List<String> second = describe(DroolsBackend.fixpointOf(b, session), beans);
                assertEquals(SECOND_VALUES, second, "second reading: the characteristic, the conditional value and the"
                        + " default lapse, the sum follows, and the initialisation fires again, since its target stayed"
                        + " empty on the bean");
                assertEquals(freshRead(b, beans), second, "the second reading is that of a fresh session");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_fault_lapses_and_a_fault_arises_between_two_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(faultDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = new LinkedHashMap<>();
            beans.put("V1", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("noemer", "2"), List.of()));
            beans.put("V2", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("noemer", "25"), List.of()));
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                assertEquals(List.of("V1: " + TmsDocuments.irrational("aandeel", "2"), "V2.aandeel = 5"),
                        describe(DroolsBackend.fixpointOf(b, session), beans), "first reading");
                set(beans.get("V1"), "noemer", "16");
                set(beans.get("V2"), "noemer", "3");
                for (Object v : beans.values()) session.update(session.getFactHandle(v), v);
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "second fire");
                List<String> second = describe(DroolsBackend.fixpointOf(b, session), beans);
                assertEquals(List.of("V1.aandeel = 4", "V2: " + TmsDocuments.irrational("aandeel", "3")), second,
                        "second reading: the fault on V1 lapsed, the one on V2 arose");
                assertEquals(freshRead(b, beans), second, "the second reading is that of a fresh session");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void after_the_write_back_fire_refuses_the_session() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                DroolsBackend.setDerivedValues(b, session);
                assertNumber("15", TmsDocuments.read(beans.get("V1"), "vindloon"), "V1 vindloon written back");
                IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, session));
                assertEquals("deze sessie is teruggezet (setDerivedValues): haar beans dragen afgeleide waarden naast"
                        + " de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                        + " DroolsBackend.fixpointOf en zet pas na de laatste fire terug, of open een verse sessie met beans die"
                        + " alleen invoer dragen", e.getMessage());
                assertEquals(FIRST_VALUES, describe(DroolsBackend.fixpointOf(b, session), beans),
                        "the reading after the write-back is the one it wrote");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_model_with_creation_refuses_a_second_fire() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        String caseDocument = Files.readString(ir.resolveSibling("case1-berekend.json"));
        CaseRunner runner = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL);
        runner.withSession(TmsDocuments.JSON.readTree(caseDocument), (build, session, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "first fire");
            assertEquals("70", heffing(DroolsBackend.fixpointOf(build, session), j1), "J1 heffing, first fire");
            try {
                set(j1, "grondslag", "150");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            session.update(session.getFactHandle(j1), j1);
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(build, session));
            assertEquals("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.fire gevuurd: wat een"
                    + " creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte initialisatie,"
                    + " is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou na een"
                    + " invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een verse"
                    + " sessie", e.getMessage());
            return null;
        });
        // the way the sentence names: a fresh session of the changed input
        JsonNode changed = TmsDocuments.JSON.readTree(caseDocument.replace("\"100\"", "\"150\""));
        runner.withSession(changed, (build, session, labels) -> {
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "fresh fire");
            assertEquals("120", heffing(DroolsBackend.fixpointOf(build, session), labels.get("J1")),
                    "J1 heffing in a fresh session of grondslag 150");
            return null;
        });
    }

    /** The reading of a fresh session of the values document in which V1's vinder is a second Jutter J2. */
    static final List<String> WITH_J2 = List.of("J1.totaalgewicht = 4", "J2.totaalgewicht = 10", "V1 is zwaar",
            "V1.bonus = 7", "V1.heffing = 40", "V1.uitkering = 20", "V1.vindloon = 15", "V2.uitkering = 14",
            "V2.vindloon = 9");

    @Test void a_relation_to_an_object_outside_the_input_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            Object v1 = beans.get("V1");
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                setRole(v1, "vinder", j2);
                session.update(session.getFactHandle(v1), v1);
                assertEquals(moreThanOneUpdate(AFTER_FIRE, relation("vinder", v1)), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, session)).getMessage(), "the second fire: a changed relation");
                assertEquals(TmsConsumerWitnessTest.outsideFire(TmsConsumerWitnessTest.name(v1)
                        + " werd gewijzigd met update", FRESH_OUTSIDE_FIRE),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(),
                        "the reading after the refused fire still sees the update");
            } finally {
                session.dispose();
            }
            // the reference set without an update: the watch sees no event, and the reference check refuses
            setRole(v1, "vinder", beans.get("J1"));
            KieSession withoutUpdate = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, withoutUpdate, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, withoutUpdate).noFixpoint(), "first fire");
                setRole(v1, "vinder", j2);
                assertEquals("vuur: het veld 'vinder' van " + TmsConsumerWitnessTest.name(v1) + " wijst naar een Jutter ("
                        + TmsConsumerWitnessTest.name(j2) + ") die niet als bean in de invoer staat: de tak leidt alleen af"
                        + " voor de objecten in de sessie, dus geef elk object waarnaar een bean wijst ook zelf als bean op",
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, withoutUpdate)).getMessage(),
                        "the second fire: the reference check");
            } finally {
                withoutUpdate.dispose();
            }
            // the way the sentence names: a fresh session that gives J2 as a bean
            beans.put("J2", j2);
            assertEquals(WITH_J2, freshRead(b, beans), "a fresh session that gives J2");
        });
    }

    @Test void input_added_or_removed_outside_insert_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            String newOrAway = "): een afnemer geeft alle beans in één Input aan insert en verandert daarna alleen hun"
                    + " attributen en kenmerken, met update, want insert legt de plaatsen van een lijst en de"
                    + " emmerscalairen vast over de beans van één invoer; een nieuw of verwijderd object vraagt een verse"
                    + " sessie";
            String afterFire = "sinds de laatste DroolsBackend.fire van deze sessie kwam er buiten insert om invoer bij of"
                    + " ging er invoer weg (";
            // J2 inserted between the fires and V1 set on it: J2 is in the session, so the reference check would pass
            Map<String, Object> beans = valueBeans(b, "10");
            Object v1 = beans.get("V1");
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                session.insert(j2);
                setRole(v1, "vinder", j2);
                session.update(session.getFactHandle(v1), v1);
                String inserted = TmsConsumerWitnessTest.name(j2) + " werd ingevoegd";
                assertEquals(afterFire + inserted + newOrAway, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, session)).getMessage(), "a bean inserted between two fires");
                assertEquals(TmsConsumerWitnessTest.outsideFire(inserted, NEW_OR_AWAY), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(),
                        "the reading sends the consumer to a fresh session");
            } finally {
                session.dispose();
            }
            // V2 deleted between the fires
            Map<String, Object> second = valueBeans(b, "10");
            Object v2 = second.get("V2");
            KieSession away = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, away, new Input().beans(second.values()));
                assertNull(DroolsBackend.fire(b, away).noFixpoint(), "first fire");
                away.delete(away.getFactHandle(v2));
                assertEquals(afterFire + TmsConsumerWitnessTest.name(v2) + " werd verwijderd" + newOrAway,
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, away)).getMessage(),
                        "a bean deleted between two fires");
            } finally {
                away.dispose();
            }
            // a Jutter inserted after insert, before the first fire
            Map<String, Object> third = valueBeans(b, "10");
            Object j3 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession vooraf = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, vooraf, new Input().beans(third.values()));
                vooraf.insert(j3);
                assertEquals("sinds DroolsBackend.insert kwam er buiten insert om invoer bij of ging er invoer weg ("
                        + TmsConsumerWitnessTest.name(j3) + " werd ingevoegd" + newOrAway,
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, vooraf)).getMessage(),
                        "a bean inserted after insert, before the first fire");
            } finally {
                vooraf.dispose();
            }
        });
    }

    /** The way on of the two readings where input came or went outside {@code insert}. */
    static final String NEW_OR_AWAY = "invoer die buiten DroolsBackend.insert om bijkwam of wegging, vraagt een verse"
            + " sessie, dus DroolsBackend.fire weigert haar: open een verse sessie met alle beans in één Input";

    /** The call of the entry the watch keeps from after a fire. */
    static final String AFTER_FIRE = "de laatste DroolsBackend.fire van deze sessie";

    /** The sentence of {@code fire} where, since {@code since}, more happened outside it than an update of an attribute
     *  or a characteristic of a bean, {@code description} naming it. */
    static String moreThanOneUpdate(String since, String description) {
        return "sinds " + since + " gebeurde er buiten fire om meer dan een update van een attribuut of kenmerk van een"
                + " bean (" + description + "), en meer laat de ingang niet toe: open een verse sessie met alle beans in één"
                + " Input";
    }

    /** The way on of the two readings where {@code fire} refuses for that reason. */
    static final String FRESH_OUTSIDE_FIRE = "DroolsBackend.fire weigert een sessie waarin buiten fire om meer gebeurde"
            + " dan een update van een attribuut of kenmerk van een bean: open een verse sessie met alle beans in één"
            + " Input";

    /** The clause of a relation that an update changed: field {@code field} of {@code beanObject}. */
    static String relation(String field, Object beanObject) {
        return "het veld '" + field + "' van " + TmsConsumerWitnessTest.name(beanObject) + ", een relatie, werd gewijzigd met"
                + " update";
    }

    @Test void a_global_that_changes_after_the_first_fire_refuses_fire_read_and_write_back() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "jutterswet-aanvoergeld", "ir.json");
        String caseDocument = Files.readString(ir.resolveSibling("case.json"));
        CaseRunner runner = CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL);
        String changed = "de global 'aanvoertarief' veranderde van 12.50 in 22.50 na de eerste DroolsBackend.fire van"
                + " deze sessie";
        runner.withSession(TmsDocuments.JSON.readTree(caseDocument), (build, session, labels) -> {
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "first fire");
            assertEquals(List.of("J1 322.5", "J2 99.9", "J3 12.5"), aanvoergeld(DroolsBackend.fixpointOf(build, session), labels),
                    "the golden's aanvoergeld at tarief 12.50");
            session.setGlobal("aanvoertarief", new BigDecimal("22.50"));
            assertEquals(changed + ": de tak leest een global één keer, in een regel die haar bij de eerste fire als"
                    + " feit stelt, dus een volgende fire zou met de oude waarde rekenen; zet de globals vóór de eerste"
                    + " fire, en open voor een andere waarde een verse sessie",
                    assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(build, session)).getMessage(),
                    "the second fire");
            String expectedSentence = "geen fixpunt om te lezen of terug te zetten: " + changed + ", en het fixpunt hoort bij de"
                    + " oude waarde; DroolsBackend.fire weigert de sessie, dus open een verse sessie";
            assertEquals(expectedSentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(build, session))
                    .getMessage(), "the reading");
            assertEquals(expectedSentence, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.setDerivedValues(build, session)).getMessage(), "the write-back");
            return null;
        });
        // the way the sentence names: a fresh session with the other value
        JsonNode another = TmsDocuments.JSON.readTree(caseDocument);
        ((com.fasterxml.jackson.databind.node.ObjectNode) another.get("input").get("parameters").get("aanvoertarief"))
                .put("number", "22.50");
        runner.withSession(another, (build, session, labels) -> {
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "fresh fire");
            assertEquals(List.of("J1 332.5", "J2 109.9", "J3 22.5"), aanvoergeld(DroolsBackend.fixpointOf(build, session), labels),
                    "a fresh session at tarief 22.50");
            return null;
        });
    }

    @Test void a_deleted_fact_of_the_branch_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                org.kie.api.runtime.rule.FactHandle h = uitkering(session, beans.get("V1"));
                Object value = session.getObject(h);
                session.delete(h);
                String description = TmsConsumerWitnessTest.name(value) + " werd verwijderd";
                assertEquals(moreThanOneUpdate(AFTER_FIRE, description), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, session)).getMessage(), "the second fire");
                assertEquals(TmsConsumerWitnessTest.outsideFire(description, FRESH_OUTSIDE_FIRE), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(), "the reading");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void an_inserted_or_changed_fact_that_is_no_bean_of_the_input_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            // a fact of no kind insert puts in
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                String foreign = "geen invoer";
                session.insert(foreign);
                assertEquals(moreThanOneUpdate(AFTER_FIRE, TmsConsumerWitnessTest.name(foreign) + " werd ingevoegd"),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, session)).getMessage(),
                        "a fact inserted between two fires");
            } finally {
                session.dispose();
            }
            // a fact of the branch updated
            Map<String, Object> second = valueBeans(b, "10");
            KieSession other = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, other, new Input().beans(second.values()));
                assertNull(DroolsBackend.fire(b, other).noFixpoint(), "first fire");
                org.kie.api.runtime.rule.FactHandle h = uitkering(other, second.get("V1"));
                other.update(h, other.getObject(h));
                assertEquals(moreThanOneUpdate(AFTER_FIRE, TmsConsumerWitnessTest.name(other.getObject(h))
                        + " werd gewijzigd met update"), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, other)).getMessage(), "a fact of the branch updated between two fires");
            } finally {
                other.dispose();
            }
        });
    }

    @Test void a_bare_fire_between_two_fires_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                Object v1 = beans.get("V1");
                set(v1, "gewicht", "3");
                session.update(session.getFactHandle(v1), v1);
                String rule = TmsConsumerWitnessTest.bareFire(session);
                assertNotNull(rule, "the plain fire fires a rule: the network has not seen V1's new gewicht");
                assertEquals(moreThanOneUpdate(AFTER_FIRE, "de regel \"" + rule + "\" vuurde"), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.fire(b, session)).getMessage(), "the fire after it");
                assertEquals(TmsConsumerWitnessTest.outsideFire(TmsConsumerWitnessTest.name(v1) + " werd gewijzigd met"
                        + " update en de regel \"" + rule + "\" vuurde", FRESH_OUTSIDE_FIRE),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(),
                        "the reading");
            } finally {
                session.dispose();
            }
        });
    }

    /** J1 actief with V1 (gewicht 9, the root 3) as its aanmelder's Vondst, and V2 (gewicht 3, no square) without an
     *  aanmelder; then V2 gets J1. Until the card zero-division-channel V1 divided 10 by 2 and V2 9 by 0. */
    @Test void a_relation_that_arises_between_two_fires_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterFaultWitnessTest.document(TmsFilterFaultWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = new LinkedHashMap<>();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            beans.put("J1", j1);
            beans.put("V1", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "9"), List.of()));
            beans.put("V2", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "3"), List.of()));
            setRole(beans.get("V1"), "aanmelder", j1);
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                assertEquals(List.of("J1.zware vondsten = 1"), describe(DroolsBackend.fixpointOf(b, session), beans),
                        "first reading: V2 is no member");
                Object v2 = beans.get("V2");
                setRole(v2, "aanmelder", j1);
                session.update(session.getFactHandle(v2), v2);
                assertEquals(moreThanOneUpdate(AFTER_FIRE, relation("aanmelder", v2)), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.fire(b, session)).getMessage(), "the second fire");
                assertEquals(TmsConsumerWitnessTest.outsideFire(TmsConsumerWitnessTest.name(v2) + " werd gewijzigd met"
                        + " update", FRESH_OUTSIDE_FIRE), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fixpointOf(b, session)).getMessage(), "the reading");
            } finally {
                session.dispose();
            }
            // the way the sentence names: a fresh session, in which V2 takes the root of 3 in J1's list
            assertEquals(List.of("J1: " + ZWARE_FAULT.formatted("3")), freshRead(b, beans), "a fresh session");
        });
    }

    /** J1 and J2 actief, V1 (gewicht 3) J1's, V2 (gewicht 2) J2's, each at place 0 of its list, the filter faulting on
     *  both; then V1 moves to J2. Until the card zero-division-channel V1 divided 9 and V2 4 by 0. */
    @Test void a_relation_that_moves_between_two_fires_refuses_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterFaultWitnessTest.document(TmsFilterFaultWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = new LinkedHashMap<>();
            Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            beans.put("J1", j1);
            beans.put("J2", j2);
            beans.put("V1", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "3"), List.of()));
            beans.put("V2", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "2"), List.of()));
            setRole(beans.get("V1"), "aanmelder", j1);
            setRole(beans.get("V2"), "aanmelder", j2);
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                assertEquals(List.of("J1: " + ZWARE_FAULT.formatted("3"), "J2: " + ZWARE_FAULT.formatted("2")),
                        describe(DroolsBackend.fixpointOf(b, session), beans), "first reading");
                Object v1 = beans.get("V1");
                setRole(v1, "aanmelder", j2);
                session.update(session.getFactHandle(v1), v1);
                assertEquals(moreThanOneUpdate(AFTER_FIRE, relation("aanmelder", v1)), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.fire(b, session)).getMessage(), "the second fire");
            } finally {
                session.dispose();
            }
            // the way the sentence names: a fresh session, in which V1 comes before V2 in J2's list and takes the root of 3
            assertEquals(List.of("J1.zware vondsten = 0", "J2: " + ZWARE_FAULT.formatted("3")), freshRead(b, beans),
                    "a fresh session");
        });
    }

    /** The clause of a relation that changed without an update: field {@code field} of {@code bean}, which held
     *  {@code before} at {@code insert} and holds {@code after}. */
    static String withoutUpdate(String field, Object bean, Object before, Object after) {
        return "het veld '" + field + "' van " + TmsConsumerWitnessTest.name(bean) + ", een relatie, veranderde zonder"
                + " update: het hield bij DroolsBackend.insert " + TmsConsumerWitnessTest.name(before) + " en houdt nu "
                + TmsConsumerWitnessTest.name(after);
    }

    /** J1 and J2 in the input, V1 (10) and V2 (4) found by J1; after the first fire V1's vinder becomes J2 without an
     *  update, which Drools does not see. */
    @Test void a_relation_that_changes_without_update_refuses_fire_read_and_write_back() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            Object j1 = beans.get("J1"), v1 = beans.get("V1");
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            beans.put("J2", j2);
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                assertEquals(FIRST_VALUES, describe(DroolsBackend.fixpointOf(b, session), beans), "first reading");
                setRole(v1, "vinder", j2);
                String changed = withoutUpdate("vinder", v1, j1, j2);
                String expectedSentence = "geen fixpunt om te lezen of terug te zetten: " + changed + ", en het fixpunt hoort bij"
                        + " de oude relatie; DroolsBackend.fire weigert de sessie, dus open een verse sessie met alle"
                        + " beans in één Input";
                assertEquals(expectedSentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session))
                        .getMessage(), "the reading");
                assertEquals(expectedSentence, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, session)).getMessage(), "the write-back");
                assertEquals(changed + ": Drools ziet een veld pas na een update, dus een fire zou de oude relatie"
                        + " blijven lezen, en ook met een update vraagt een gewijzigde relatie een verse sessie, want"
                        + " insert legt de plaatsen van een lijst vast over de relaties van één invoer; open een verse"
                        + " sessie met alle beans in één Input", assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, session)).getMessage(), "the second fire");
            } finally {
                session.dispose();
            }
            // the way the sentences name: a fresh session of the changed input
            assertEquals(WITH_J2, freshRead(b, beans), "a fresh session");
        });
    }

    @Test void an_emptied_agenda_or_a_focus_switch_outside_fire_refuses_fire_read_and_write_back()
            throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        String main = "de agendagroep \"MAIN\" werd geleegd";
        inSession(b, () -> {
            // the agenda cleared after insert, before the first fire
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                session.getAgenda().clear();
                assertEquals(moreThanOneUpdate("DroolsBackend.insert", main), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, session)).getMessage(), "the fire after the agenda was cleared");
            } finally {
                session.dispose();
            }
            // a group cleared after the fire
            Map<String, Object> second = valueBeans(b, "10");
            KieSession other = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, other, new Input().beans(second.values()));
                assertNull(DroolsBackend.fire(b, other).noFixpoint(), "first fire");
                other.getAgenda().getAgendaGroup("MAIN").clear();
                // Drools queues the clear of a group for the next fire, which ran it with an
                // UnsupportedOperationException (measured 2026-10-10), so the watch names the call that waits
                String group = "een agendagroep werd geleegd met AgendaGroup.clear";
                String expectedSentence = TmsConsumerWitnessTest.outsideFire(group, FRESH_OUTSIDE_FIRE);
                assertEquals(expectedSentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, other))
                        .getMessage(), "the reading");
                assertEquals(expectedSentence, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, other)).getMessage(), "the write-back");
                assertEquals(moreThanOneUpdate(AFTER_FIRE, group), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.fire(b, other)).getMessage(), "the second fire");
            } finally {
                other.dispose();
            }
        });
        // a focus change after the fire, on a model with a creation, the only kind whose KieBase has a group beside MAIN
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        CaseRunner.forMeasurement(ir, DroolsBackend.Branch.EXEC_MODEL).withSession(TmsDocuments.JSON.readTree(
                Files.readString(ir.resolveSibling("case1-berekend.json"))), (build, session, labels) -> {
            assertNull(DroolsBackend.fire(build, session).noFixpoint(), "first fire");
            session.getAgenda().getAgendaGroup(dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.OBJECT_CREATION)
                    .setFocus();
            String expectedSentence = TmsConsumerWitnessTest.outsideFire("een agendagroep kreeg de focus met"
                    + " AgendaGroup.setFocus", TmsConsumerWitnessTest.FRESH_AT_CREATION);
            assertEquals(expectedSentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(build, session))
                    .getMessage(), "the reading");
            assertEquals(expectedSentence, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.setDerivedValues(build, session)).getMessage(), "the write-back");
            return null;
        });
    }

    /** An action on the propagation list of a kind the watch does not know, as another version of Drools could queue
     *  for the next fire; it records whether a fire ran it. */
    private static final class UnknownAction extends org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry {
        boolean executed;

        @Override public void internalExecute(org.drools.core.common.ReteEvaluator evaluator) {
            executed = true;
        }
    }

    @Test void an_unknown_action_on_the_propagation_list_refuses_fire_read_and_write_back() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                UnknownAction action = new UnknownAction();
                ((org.drools.core.common.InternalAgenda) session.getAgenda()).addPropagation(action);
                String sentence = "op de propagatielijst van deze sessie wacht een " + UnknownAction.class.getName()
                        + ", een actie die de bewaking niet kent: zij kent naast een insert, update of delete van een"
                        + " feit alleen die van AgendaGroup.clear en AgendaGroup.setFocus, dus ze kan niet zeggen of een"
                        + " afnemer de agenda veranderde";
                assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session))
                        .getMessage(), "the reading");
                assertEquals(sentence, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.setDerivedValues(b, session)).getMessage(), "the write-back");
                assertEquals(sentence, assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, session))
                        .getMessage(), "the second fire");
                assertFalse(action.executed, "the refused fire ran no action");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_fired_session_without_its_watch_refuses_fire_and_read() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(valueDocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSession(b, () -> {
            Map<String, Object> beans = valueBeans(b, "10");
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans.values()));
                assertNull(DroolsBackend.fire(b, session).noFixpoint(), "first fire");
                for (org.kie.api.event.rule.RuleRuntimeEventListener l : List.copyOf(session.getRuleRuntimeEventListeners()))
                    session.removeEventListener(l);
                assertEquals("de bewaking die DroolsBackend.fire op deze sessie zette, staat er niet meer op, dus wat er"
                        + " na de laatste fire gebeurde, is niet te zien; open een verse sessie met alle beans in één"
                        + " Input", assertThrows(IllegalStateException.class, () -> DroolsBackend.fire(b, session))
                        .getMessage(), "the second fire");
                assertEquals("geen fixpunt om te lezen of terug te zetten: de bewaking die DroolsBackend.fire op deze"
                        + " sessie zet, staat er niet meer op, dus wat er na de laatste fire gebeurde, is niet te zien;"
                        + " DroolsBackend.fire weigert haar ook: open een verse sessie met alle beans in één Input",
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.fixpointOf(b, session)).getMessage(),
                        "the reading");
            } finally {
                session.dispose();
            }
        });
    }

    /** The fault of the rule "zware vondsten" where the filter takes the root of {@code %s}, as {@code Fixpoint} gives
     *  it. */
    static final String ZWARE_FAULT = TmsDocuments.irrational("zware vondsten", "%s");

    /** The handle of the {@code Value} that gives {@code vondst} its uitkering. */
    private static org.kie.api.runtime.rule.FactHandle uitkering(KieSession session, Object vondst) {
        List<org.kie.api.runtime.rule.FactHandle> out = new ArrayList<>(session.getFactHandles(o ->
                o instanceof dev.uchar.regelspraak.transpiler.tms.Value w && w.subject() == vondst
                        && w.attribute().name().equals("uitkering")));
        assertEquals(1, out.size(), "the Waarde of V1's uitkering");
        return out.get(0);
    }

    /** The aanvoergeld {@code f} derives, per label of {@code labels}, as "J1 322.5". */
    private static List<String> aanvoergeld(Fixpoint f, Map<String, Object> labels) {
        List<String> out = new ArrayList<>();
        labels.forEach((label, beanObject) -> {
            for (Fixpoint.DerivedValue w : f.values())
                if (w.object() == beanObject && w.attribute().equals("aanvoergeld"))
                    out.add(label + " " + ((BigDecimal) w.value()).stripTrailingZeros().toPlainString());
        });
        out.sort(null);
        return out;
    }

    // ---------------- helpers ----------------

    /** The heffing {@code f} derives for {@code jutter}, as a plain number, or null. */
    private static String heffing(Fixpoint f, Object jutter) {
        for (Fixpoint.DerivedValue w : f.values())
            if (w.object() == jutter && w.attribute().equals("heffing"))
                return ((BigDecimal) w.value()).stripTrailingZeros().toPlainString();
        return null;
    }

    /** J1, and V1 of {@code weightV1} and V2 of 4, each found by J1. */
    static Map<String, Object> valueBeans(DroolsBackend.Build b, String weightV1) throws Exception {
        Map<String, Object> beans = new LinkedHashMap<>();
        Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
        beans.put("J1", j1);
        beans.put("V1", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", weightV1), List.of()));
        beans.put("V2", TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "4"), List.of()));
        for (String v : List.of("V1", "V2")) setRole(beans.get(v), "vinder", j1);
        return beans;
    }

    /** The reading of a fresh session of {@code b} over the same beans, which hold the input alone. */
    private static List<String> freshRead(DroolsBackend.Build b, Map<String, Object> beans) {
        KieSession fresh = b.kieBase().newKieSession();
        try {
            DroolsBackend.insert(b, fresh, new Input().beans(beans.values()));
            assertNull(DroolsBackend.fire(b, fresh).noFixpoint(), "fresh fire");
            return describe(DroolsBackend.fixpointOf(b, fresh), beans);
        } finally {
            fresh.dispose();
        }
    }

    /** {@code f} as sorted lines over the labels of {@code beans}: "V1.uitkering = 20", "V1 is zwaar", "V1: &lt;fault&gt;",
     *  "gemaakt: …", "koppeling: …" and "inconsistent: …". */
    static List<String> describe(Fixpoint f, Map<String, Object> beans) {
        Map<Object, String> label = new IdentityHashMap<>();
        beans.forEach((l, beanObject) -> label.put(beanObject, l));
        List<String> out = new ArrayList<>();
        for (Fixpoint.DerivedValue w : f.values())
            out.add(label.get(w.object()) + "." + w.attribute() + " = "
                    + (w.value() instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : w.value()));
        for (Fixpoint.DerivedCharacteristic k : f.characteristics()) out.add(label.get(k.object()) + " is " + k.characteristic());
        for (Fixpoint.MadeObject o : f.objects())
            out.add("gemaakt: " + o.creation() + " voor " + label.get(o.subject()));
        for (Fixpoint.MadeLink k : f.links())
            out.add("koppeling: " + k.factType() + " " + label.get(k.first()) + " " + label.get(k.second()));
        for (Fault fault : f.faults()) out.add(label.get(fault.subject()) + ": " + fault.message());
        for (Object i : f.inconsistencies()) out.add("inconsistent: " + i);
        out.sort(null);
        return out;
    }

    static void set(Object beanObject, String attribute, String lexeme) throws Exception {
        beanObject.getClass().getMethod(Namen.setter(attribute), BigDecimal.class).invoke(beanObject, new BigDecimal(lexeme));
    }

    static void setRole(Object beanObject, String role, Object target) throws Exception {
        for (java.lang.reflect.Method m : beanObject.getClass().getMethods())
            if (m.getName().equals(Namen.setter(role)) && m.getParameterCount() == 1) {
                m.invoke(beanObject, target);
                return;
            }
        throw new AssertionError("geen setter voor " + role + " op " + beanObject.getClass());
    }

    /** Work that may throw, run with the bean loader of the build as the thread's context class loader. */
    interface Work {
        void doe() throws Exception;
    }

    static void inSession(DroolsBackend.Build b, Work work) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            work.doe();
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static void assertNumber(String expected, Object actual, String description) {
        assertTrue(actual instanceof BigDecimal w && new BigDecimal(expected).compareTo(w) == 0,
                () -> description + ": verwacht " + expected + ", werkelijk " + actual);
    }
}
