package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.tms.HasCharacteristic;
import dev.uchar.regelspraak.transpiler.tms.TextOrder;
import dev.uchar.regelspraak.transpiler.tms.Value;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.attributeGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.decision;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.beanObject;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.has;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.jutterBean;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicGroup;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.lees;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.standaard;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The write-back of the exec-model branch ({@code DroolsBackend.zetAfgeleideWaarden}). The branch derives with
 * logical inserts and never writes a bean while it fires, so a derived characteristic reaches the bean through this call
 * only, after the fire. "Een Jutter is vergund indien hij erkend is" over J1 (erkend in the input), J2 (vergund in the
 * input, not erkend) and J3 (neither): before the call J1's bean is not vergund, though the session holds the
 * derivation; after it J1 is vergund and still erkend, J2 stays vergund, since input on a characteristic stands and
 * nothing writes false, and J3 carries neither. On the DRL-text branch the call writes nothing: its rules write the
 * beans while they fire.
 *
 * <p>The same holds for every other writer the branch carries, which a second document puts on one Vondst type: "De
 * uitkering van een Vondst moet berekend worden als zijn gewicht maal 2", "Het vindloon van een Vondst moet
 * geïnitialiseerd worden op zijn gewicht plus 5", a defaults group that gives the heffing 40 where the Vondst is zwaar,
 * and a defaults group that decides vrijgesteld do-not-attribute where it is ny and attribute where it is nx. V1
 * (gewicht 10, heffing 7, zwaar, ny, nx and vrijgesteld in the input): uitkering 20, vindloon 15, heffing 40, since a
 * deciding default replaces the input on an attribute, and vrijgesteld, since the decision not to attribute is no
 * retraction and the input on a characteristic stands against it (RUNTIME-SEMANTICS ch. 7.1 and 7.8). V2 (gewicht 10,
 * vindloon 3, heffing 7 and nx in the input): uitkering 20, and vindloon 3 and heffing 7 as input, which no writer
 * replaces; vrijgesteld by the lower default. V3 (gewicht 4): uitkering 8, vindloon 9, heffing leeg, not vrijgesteld.
 * Before the call each bean holds its input while the session holds the derivations; every order of the two rules and
 * of the two groups in the document. Two different values on one attribute of one object are a named failure that
 * writes neither value, the same sentence whichever of the two facts the session took first; equal values, 20 and
 * 20.0, are one value.
 *
 * <p><b>Red before the write-back</b> (measured 2026-10-08). On the branch's skeleton the first two rows were refused,
 * "tms-transpiler SKIP: regel: vergund indien erkend — de verlaging van een kenmerktoekenning is nog niet gebouwd". With
 * the lowering in place and the write-back switched off, the replay answered "case: kenmerk vergund: verwacht aanwezig,
 * werkelijk afwezig" and the direct fire failed on "J1 vergund ==&gt; expected: &lt;true&gt; but was: &lt;false&gt;",
 * after the session had shown the HasCharacteristic of J1. The row on the other two branches was green from the start.
 *
 * <p><b>Red by mutation of the write-back</b> (measured 2026-10-08 when the rows of the second document and of the two
 * writers were added, the write-back being in place; the mutants are not committed). Without the write-back of a
 * {@link Value}, the replay failed on "in documentvolgorde [uitkering, standaardvindloon] [heffing van een vondst,
 * vrijstelling van een vondst] ==&gt; expected: &lt;null&gt; but was: &lt;case: attribuut uitkering: verwacht 20,
 * werkelijk leeg; attribuut vindloon: verwacht 15, werkelijk leeg; attribuut heffing: verwacht 40, werkelijk 7;
 * attribuut uitkering: verwacht 20, werkelijk leeg; attribuut uitkering: verwacht 8, werkelijk leeg; attribuut
 * vindloon: verwacht 9, werkelijk leeg&gt;", the direct fire on "V1 uitkering in documentvolgorde […]: verwacht 20,
 * werkelijk null", and the row of equal values on "V1 uitkering: verwacht 20, werkelijk null". With a write-back that
 * leaves an attribute the input set, the replay failed on "… but was: &lt;case: attribuut heffing: verwacht 40,
 * werkelijk 7&gt;" and the direct fire on "V1 heffing in documentvolgorde […]: verwacht 40, werkelijk 7". With a
 * write-back that writes every derived characteristic as the session holds it, absence as false, the replays failed on
 * "… but was: &lt;case: kenmerk vrijgesteld: verwacht aanwezig, werkelijk afwezig&gt;" and "… but was: &lt;case:
 * kenmerk vergund: verwacht aanwezig, werkelijk afwezig&gt;", the direct fires on "J2 vergund, the input, stays" and
 * "V1 vrijgesteld, the input, stays in documentvolgorde […]". Without the comparison of the two values, the
 * divergence row failed on "a failure in each order, got [], V1 uitkering 20 ==&gt; expected: &lt;2&gt; but was:
 * &lt;0&gt;": the write-back had chosen one value. With the values compared by {@code equals}, the row of equal
 * values failed on "expected: &lt;[]&gt; but was: &lt;[geen fixpunt: twee schrijvers van één doel geven verschillende
 * waarden — de regel "uitkering" schrijft 20 en de regel "uitkering volgens een tweede regel" schrijft 20.0 in het
 * attribuut uitkering van een Vondst (Vondst@…); divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7), …]&gt;".
 *
 * <p><b>Both branches of the analysis</b> (card drl-teksttak-op-truth-maintenance, rest of step 4; lens on round 4:
 * these orders of rules and defaults groups ran on the executable model only): every row but the one of the DRL-text
 * branch runs on the exec-model branch and on {@code DRL_TEXT}, its rendering as DRL text, whose write-back is the same
 * code; and a seeded sample of the orders of the generated rules fires the Vondsten and writes back on both, on
 * {@code DRL_TEXT} with the rules of the text in each order ({@code TextOrder}). The sample passes no intermediate
 * standing: each position of the two groups reads an input characteristic, whose standing is fixed before the fire, so
 * no lower default decides before a higher one (measured on the exec-model branch: no order of the sample withdrew a
 * control fact); it pins the write-back in every order the sample takes. <b>Red</b> on {@code e2bd685e}, before step 4: every row with the two
 * defaults groups failed on
 * {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet"; the rows of the characteristic rule alone
 * were green there from the start, since step 2a carries that rule.
 */
class TmsWriteBackWitnessTest {

    private static final List<String> CHARACTERISTICS = List.of("erkend", "vergund");

    /** The two branches with truth maintenance, whose write-back is one implementation. */
    private static final List<DroolsBackend.Branch> BRANCHES = List.of(DroolsBackend.Branch.EXEC_MODEL,
            DroolsBackend.Branch.DRL_TEXT);

    @Test void the_replay_reads_the_derived_and_the_input_characteristics() throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES)
            assertNull(CaseRunner.playCase(document(), TmsDocuments.caseDocument(
                    List.of(TmsDocuments.jutter("J1", List.of("erkend")), TmsDocuments.jutter("J2", List.of("vergund")),
                            TmsDocuments.jutter("J3", List.of())),
                    List.of(verwacht("J1", "vergund", true), verwacht("J1", "erkend", true),
                            verwacht("J2", "vergund", true), verwacht("J2", "erkend", false),
                            verwacht("J3", "vergund", false), verwacht("J3", "erkend", false))), branch), branch.toString());
    }

    @Test void only_after_the_write_back_the_bean_carries_the_derived_characteristic() throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES) setCharacteristicBack(DroolsBackend.build(document(), branch));
    }

    private static void setCharacteristicBack(DroolsBackend.Build b) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            Object j1 = jutterBean(b.beanLoader(), List.of("erkend"));
            Object j2 = jutterBean(b.beanLoader(), List.of("vergund"));
            Object j3 = jutterBean(b.beanLoader(), List.of());
            for (Object j : List.of(j1, j2, j3)) sessie.insert(j);
            String branch = b.branch() + ": ";
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), branch + "the fixpoint");
            // the derivation is a fact of the session, and the bean does not carry it yet
            List<Object> derived = new ArrayList<>(sessie.getObjects(o -> o instanceof HasCharacteristic));
            assertEquals(List.of(new HasCharacteristic(j1, ElementSleutel.van("Jutter", "vergund", true))), derived,
                    branch + "the derivation");
            assertFalse(has(j1, "vergund"), branch + "J1 is vergund before the write-back");
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertTrue(has(j1, "vergund"), branch + "J1 vergund");
            assertTrue(has(j1, "erkend"), branch + "J1 erkend, the input, stays");
            assertTrue(has(j2, "vergund"), branch + "J2 vergund, the input, stays");
            assertFalse(has(j2, "erkend"), branch + "J2 erkend");
            assertFalse(has(j3, "vergund"), branch + "J3 vergund");
            assertFalse(has(j3, "erkend"), branch + "J3 erkend");
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Path document() throws Exception {
        return TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS),
                List.of(characteristicRule("vergund indien erkend", "vergund", check("erkend", false))));
    }

    // ---------------- attributes: a computation, an initialisation and two defaults groups ----------------

    private static final String VONDST = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
            List.of(attribuut("gewicht", WHOLE), attribuut("uitkering", WHOLE), attribuut("vindloon", WHOLE),
                    attribuut("heffing", WHOLE)),
            List.of("zwaar", "nx", "ny", "vrijgesteld"))), List.of());

    private static final List<String> RULES = List.of(
            computingRule("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binary("times", ownRead("gewicht", "Vondst"), number("2")), "null"),
            computingRule("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon",
                    binary("plus", ownRead("gewicht", "Vondst"), number("5")), "null"));

    private static final List<String> GROUPS = List.of(
            attributeGroup("heffing van een vondst", "Vondst", "heffing",
                    List.of(standaard(check("zwaar", false), number("40")))),
            characteristicGroup("vrijstelling van een vondst", "Vondst", "vrijgesteld",
                    List.of(decision(check("ny", false), false), decision(check("nx", false), true))));

    /** The input of V1, V2 and V3, as the case sets it and as a witness that fires a session itself sets it. */
    private static final List<Map<String, String>> NUMBERS = List.of(Map.of("gewicht", "10", "heffing", "7"),
            Map.of("gewicht", "10", "vindloon", "3", "heffing", "7"), Map.of("gewicht", "4"));
    private static final List<List<String>> INPUT_CHARACTERISTICS = List.of(List.of("zwaar", "ny", "nx", "vrijgesteld"),
            List.of("nx"), List.of());

    @Test void the_replay_reads_the_derived_and_the_input_attributes_in_every_document_order() throws Exception {
        int orders = 0;
        for (List<String> rules : TmsDocuments.permutations(RULES))
            for (List<String> groups : TmsDocuments.permutations(GROUPS))
                for (DroolsBackend.Branch branch : BRANCHES) {
                    String where = " op " + branch + documentOrder(rules, groups);
                    assertNull(CaseRunner.playCase(vondstdocument(rules, groups), vondstCase(), branch), where);
                    orders++;
                }
        assertEquals(8, orders);
    }

    @Test void only_after_the_write_back_the_beans_carry_the_derived_attributes() throws Exception {
        int orders = 0;
        for (List<String> rules : TmsDocuments.permutations(RULES))
            for (List<String> groups : TmsDocuments.permutations(GROUPS))
                for (DroolsBackend.Branch branch : BRANCHES) {
                    DroolsBackend.Build b = DroolsBackend.build(vondstdocument(rules, groups), branch);
                    fireAndSetBack(b, b.kieBase(), " op " + branch + documentOrder(rules, groups));
                    orders++;
                }
        assertEquals(8, orders);
    }

    /** The same write-back in a seeded sample of the orders of the generated rules, on the exec-model branch and on
     *  {@code DRL_TEXT} with the rules of its text in each order. */
    @Test void sample_of_orders_of_the_generated_rules() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(vondstdocument(RULES, GROUPS), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int orders = 0;
        for (List<Rule> order : TmsDocuments.sample(gegenereerd, 20261013L, 100)) {
            fireAndSetBack(b, TmsDocuments.kieBase(b, order), " op " + b.branch() + " in volgorde "
                    + order.stream().map(Rule::getName).toList());
            orders++;
        }
        DroolsBackend.Build t = DroolsBackend.build(vondstdocument(RULES, GROUPS), DroolsBackend.Branch.DRL_TEXT);
        List<String> rules = TextOrder.rules(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), rules, "de regels van de tekst");
        for (List<String> order : TmsDocuments.sample(rules, 20261013L, 100)) {
            fireAndSetBack(t, TextOrder.kieBase(t, order), " op " + t.branch() + " in volgorde " + order);
            orders++;
        }
        assertEquals(2 * 102, orders);
    }

    /** Fires V1, V2 and V3 through {@code kieBase}, which holds the rules of {@code b}, and writes back: before the
     *  write-back the session holds the derivations and each bean its input only, after it each bean carries what the
     *  class comment says. */
    private static void fireAndSetBack(DroolsBackend.Build b, KieBase kieBase, String where) throws Exception {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kieBase.newKieSession();
        try {
            List<Object> v = new ArrayList<>();
            for (int i = 0; i < NUMBERS.size(); i++)
                v.add(beanObject(b.beanLoader(), "Vondst", NUMBERS.get(i), INPUT_CHARACTERISTICS.get(i)));
            for (Object beanObject : v) sessie.insert(beanObject);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), "geen fixpunt" + where);
            // the session holds the derivations, and each bean still holds its input only
            assertNumber("40", sessionValue(sessie, v.get(0), "heffing"), "V1 heffing in the session" + where);
            assertNumber("15", sessionValue(sessie, v.get(0), "vindloon"), "V1 vindloon in the session" + where);
            assertNumber("20", sessionValue(sessie, v.get(0), "uitkering"), "V1 uitkering in the session" + where);
            assertTrue(sessie.getObjects(o -> o instanceof HasCharacteristic h && h.onderwerp() == v.get(1)).size() == 1,
                    "V2 vrijgesteld in the session" + where);
            assertNull(lees(v.get(0), "uitkering"), "V1 uitkering before the write-back" + where);
            assertNull(lees(v.get(0), "vindloon"), "V1 vindloon before the write-back" + where);
            assertNumber("7", lees(v.get(0), "heffing"), "V1 heffing before the write-back" + where);
            assertFalse(has(v.get(1), "vrijgesteld"), "V2 vrijgesteld before the write-back" + where);
            assertNull(lees(v.get(2), "uitkering"), "V3 uitkering before the write-back" + where);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertNumber("20", lees(v.get(0), "uitkering"), "V1 uitkering" + where);
            assertNumber("15", lees(v.get(0), "vindloon"), "V1 vindloon" + where);
            assertNumber("40", lees(v.get(0), "heffing"), "V1 heffing" + where);
            assertTrue(has(v.get(0), "vrijgesteld"), "V1 vrijgesteld, the input, stays" + where);
            assertNumber("20", lees(v.get(1), "uitkering"), "V2 uitkering" + where);
            assertNumber("3", lees(v.get(1), "vindloon"), "V2 vindloon, the input, stays" + where);
            assertNumber("7", lees(v.get(1), "heffing"), "V2 heffing, the input, stays" + where);
            assertTrue(has(v.get(1), "vrijgesteld"), "V2 vrijgesteld" + where);
            assertNumber("8", lees(v.get(2), "uitkering"), "V3 uitkering" + where);
            assertNumber("9", lees(v.get(2), "vindloon"), "V3 vindloon" + where);
            assertNull(lees(v.get(2), "heffing"), "V3 heffing" + where);
            assertFalse(has(v.get(2), "vrijgesteld"), "V3 vrijgesteld" + where);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Two writers of one attribute of one object with different values have no common fixpoint, and the write-back
     *  does not choose: the facts are put in the session by hand, since the branch refuses every document that could
     *  derive them (two computing rules, or two initialisations, on one target), and in both orders. */
    @Test void two_different_values_on_a_target_are_a_named_fault_in_every_order() throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES) {
            List<String> sentences = new ArrayList<>();
            Object v1 = twoWriters(branch, new BigDecimal("20"), new BigDecimal("7"), sentences);
            Object written = lees(v1, "uitkering");
            assertEquals(2, sentences.size(), () -> branch + ": a failure in each order, got " + sentences + ", V1 uitkering "
                    + written);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel"
                    + " \"uitkering\" schrijft 20 en de regel \"uitkering volgens een tweede regel\" schrijft 7 in het"
                    + " attribuut uitkering van een Vondst (Vondst@" + Integer.toHexString(System.identityHashCode(v1))
                    + "); divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)", sentences.get(0), branch.toString());
            assertEquals(sentences.get(0), sentences.get(1), branch + ": the sentence does not depend on the order of the two"
                    + " facts");
            assertNull(written, branch + ": a divergence writes neither value");
        }
    }

    @Test void equal_values_of_two_writers_are_a_value() throws Exception {
        for (DroolsBackend.Branch branch : BRANCHES) {
            List<String> sentences = new ArrayList<>();
            Object v1 = twoWriters(branch, new BigDecimal("20"), new BigDecimal("20.0"), sentences);
            assertEquals(List.of(), sentences, branch.toString());
            assertNumber("20", lees(v1, "uitkering"), branch + ": V1 uitkering");
        }
    }

    /** Puts a Waarde of {@code eerste} by the rule "uitkering" and one of {@code tweede} by another writer on the
     *  uitkering of one Vondst, in both orders, each in a fresh session of {@code branch}, writes back, and collects the
     *  failures. */
    private static Object twoWriters(DroolsBackend.Branch branch, BigDecimal eerste, BigDecimal tweede,
                                         List<String> sentences) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(vondstdocument(RULES, GROUPS), branch);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            Object v1 = beanObject(b.beanLoader(), "Vondst", Map.of(), List.of());
            ElementSleutel uitkering = ElementSleutel.van("Vondst", "uitkering", false);
            Value a = new Value(v1, uitkering, eerste, "uitkering");
            Value z = new Value(v1, uitkering, tweede, "uitkering volgens een tweede regel");
            for (List<Value> order : List.of(List.of(a, z), List.of(z, a))) {
                KieSession sessie = b.kieBase().newKieSession();
                try {
                    order.forEach(sessie::insert);
                    // no rule reads a Waarde without its Vondst in the session, so the fire leaves the two as they are
                    assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "the fixpoint");
                    try {
                        DroolsBackend.zetAfgeleideWaarden(b, sessie);
                    } catch (IllegalStateException e) {
                        sentences.add(e.getMessage());
                    }
                } finally {
                    sessie.dispose();
                }
            }
            return v1;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Path vondstdocument(List<String> rules, List<String> groups) throws Exception {
        return TmsDocuments.document(VONDST, rules, List.of(), groups);
    }

    private static JsonNode vondstCase() throws Exception {
        List<String> instanties = new ArrayList<>();
        for (int i = 0; i < NUMBERS.size(); i++)
            instanties.add(instance("V" + (i + 1), "Vondst", NUMBERS.get(i), INPUT_CHARACTERISTICS.get(i), Map.of()));
        return TmsDocuments.caseDocument(instanties,
                List.of(waarde("V1", "uitkering", "20"), waarde("V1", "vindloon", "15"), waarde("V1", "heffing", "40"),
                        verwacht("V1", "vrijgesteld", true),
                        waarde("V2", "uitkering", "20"), waarde("V2", "vindloon", "3"), waarde("V2", "heffing", "7"),
                        verwacht("V2", "vrijgesteld", true),
                        waarde("V3", "uitkering", "8"), waarde("V3", "vindloon", "9"), waarde("V3", "heffing", null),
                        verwacht("V3", "vrijgesteld", false)));
    }

    private static String documentOrder(List<String> rules, List<String> groups) throws Exception {
        return " in documentvolgorde " + TmsDocuments.ruleNames(rules) + " " + TmsDocuments.ruleNames(groups);
    }

    /** The value of the one {@link Value} the session holds on {@code attribuut} of {@code beanObject}, or null. */
    private static Object sessionValue(KieSession sessie, Object beanObject, String attribuut) {
        List<Object> w = new ArrayList<>(sessie.getObjects(o -> o instanceof Value x && x.onderwerp() == beanObject
                && x.attribuut().naam().equals(attribuut)));
        assertTrue(w.size() <= 1, () -> "meer dan één Waarde op " + attribuut + ": " + w);
        return w.isEmpty() ? null : ((Value) w.get(0)).waarde();
    }

    private static void assertNumber(String verwacht, Object werkelijk, String description) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> description + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
