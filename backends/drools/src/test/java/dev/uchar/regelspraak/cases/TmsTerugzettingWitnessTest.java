package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.HeeftKenmerk;
import dev.uchar.regelspraak.transpiler.tms.Tekstvolgorde;
import dev.uchar.regelspraak.transpiler.tms.Waarde;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.besluit;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.boon;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.heeft;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.jutterBoon;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lees;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
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
 * after the session had shown the HeeftKenmerk of J1. The row on the other two branches was green from the start.
 *
 * <p><b>Red by mutation of the write-back</b> (measured 2026-10-08 when the rows of the second document and of the two
 * writers were added, the write-back being in place; the mutants are not committed). Without the write-back of a
 * {@link Waarde}, the replay failed on "in documentvolgorde [uitkering, standaardvindloon] [heffing van een vondst,
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
 * branch runs on the exec-model branch and on {@code DRL_TMS}, its rendering as DRL text, whose write-back is the same
 * code; and a seeded sample of the orders of the generated rules fires the Vondsten and writes back on both, on
 * {@code DRL_TMS} with the rules of the text in each order ({@code Tekstvolgorde}). The sample passes no intermediate
 * standing: each position of the two groups reads an input characteristic, whose standing is fixed before the fire, so
 * no lower default decides before a higher one (measured on the exec-model branch: no order of the sample withdrew a
 * control fact); it pins the write-back in every order the sample takes. <b>Red</b> on {@code e2bd685e}, before step 4: every row with the two
 * defaults groups failed on
 * {@code DRL_TMS} with "drl-tms: de DRL-weergave draagt Afloop nog niet"; the rows of the characteristic rule alone
 * were green there from the start, since step 2a carries that rule.
 */
class TmsTerugzettingWitnessTest {

    private static final List<String> KENMERKEN = List.of("erkend", "vergund");

    /** The two branches with truth maintenance, whose write-back is one implementation. */
    private static final List<DroolsBackend.Branch> TAKKEN = List.of(DroolsBackend.Branch.EXEC_MODEL,
            DroolsBackend.Branch.DRL_TMS);

    @Test void de_replay_leest_het_afgeleide_en_de_ingevoerde_kenmerken() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN)
            assertNull(CaseRunner.speelCase(document(), TmsDocumenten.zaak(
                    List.of(TmsDocumenten.jutter("J1", List.of("erkend")), TmsDocumenten.jutter("J2", List.of("vergund")),
                            TmsDocumenten.jutter("J3", List.of())),
                    List.of(verwacht("J1", "vergund", true), verwacht("J1", "erkend", true),
                            verwacht("J2", "vergund", true), verwacht("J2", "erkend", false),
                            verwacht("J3", "vergund", false), verwacht("J3", "erkend", false))), tak), tak.toString());
    }

    @Test void pas_na_het_terugzetten_draagt_de_bean_het_afgeleide_kenmerk() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN) zetKenmerkTerug(DroolsBackend.build(document(), tak));
    }

    private static void zetKenmerkTerug(DroolsBackend.Build b) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = b.kieBase().newKieSession();
        try {
            Object j1 = jutterBoon(b.beanLoader(), List.of("erkend"));
            Object j2 = jutterBoon(b.beanLoader(), List.of("vergund"));
            Object j3 = jutterBoon(b.beanLoader(), List.of());
            for (Object j : List.of(j1, j2, j3)) sessie.insert(j);
            String tak = b.branch() + ": ";
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), tak + "the fixpoint");
            // the derivation is a fact of the session, and the bean does not carry it yet
            List<Object> afgeleid = new ArrayList<>(sessie.getObjects(o -> o instanceof HeeftKenmerk));
            assertEquals(List.of(new HeeftKenmerk(j1, ElementSleutel.van("Jutter", "vergund", true))), afgeleid,
                    tak + "the derivation");
            assertFalse(heeft(j1, "vergund"), tak + "J1 is vergund before the write-back");
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertTrue(heeft(j1, "vergund"), tak + "J1 vergund");
            assertTrue(heeft(j1, "erkend"), tak + "J1 erkend, the input, stays");
            assertTrue(heeft(j2, "vergund"), tak + "J2 vergund, the input, stays");
            assertFalse(heeft(j2, "erkend"), tak + "J2 erkend");
            assertFalse(heeft(j3, "vergund"), tak + "J3 vergund");
            assertFalse(heeft(j3, "erkend"), tak + "J3 erkend");
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** On the DRL-text branch the rule writes J1 vergund while it fires; the witness takes the value off the bean again
     *  and the write-back leaves it so. The destructive exec-model branch did the same until card tms-omschakeling. */
    @Test void op_de_drl_teksttak_zet_de_terugzetting_niets() throws Exception {
        for (DroolsBackend.Branch tak : List.of(DroolsBackend.Branch.DRL_TEXT)) {
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

    // ---------------- attributes: a computation, an initialisation and two defaults groups ----------------

    private static final String VONDST = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("gewicht", GEHEEL), attribuut("uitkering", GEHEEL), attribuut("vindloon", GEHEEL),
                    attribuut("heffing", GEHEEL)),
            List.of("zwaar", "nx", "ny", "vrijgesteld"))), List.of());

    private static final List<String> REGELS = List.of(
            rekenregel("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binair("times", lezing("gewicht", "Vondst"), getal("2")), "null"),
            rekenregel("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon",
                    binair("plus", lezing("gewicht", "Vondst"), getal("5")), "null"));

    private static final List<String> GROEPEN = List.of(
            attribuutgroep("heffing van een vondst", "Vondst", "heffing",
                    List.of(standaard(check("zwaar", false), getal("40")))),
            kenmerkgroep("vrijstelling van een vondst", "Vondst", "vrijgesteld",
                    List.of(besluit(check("ny", false), false), besluit(check("nx", false), true))));

    /** The input of V1, V2 and V3, as the case sets it and as a witness that fires a session itself sets it. */
    private static final List<Map<String, String>> GETALLEN = List.of(Map.of("gewicht", "10", "heffing", "7"),
            Map.of("gewicht", "10", "vindloon", "3", "heffing", "7"), Map.of("gewicht", "4"));
    private static final List<List<String>> INGEVOERDE_KENMERKEN = List.of(List.of("zwaar", "ny", "nx", "vrijgesteld"),
            List.of("nx"), List.of());

    @Test void de_replay_leest_de_afgeleide_en_de_ingevoerde_attributen_in_elke_documentvolgorde() throws Exception {
        int volgordes = 0;
        for (List<String> regels : TmsDocumenten.permutaties(REGELS))
            for (List<String> groepen : TmsDocumenten.permutaties(GROEPEN))
                for (DroolsBackend.Branch tak : TAKKEN) {
                    String waar = " op " + tak + documentvolgorde(regels, groepen);
                    assertNull(CaseRunner.speelCase(vondstdocument(regels, groepen), vondstzaak(), tak), waar);
                    volgordes++;
                }
        assertEquals(8, volgordes);
    }

    @Test void pas_na_het_terugzetten_dragen_de_beans_de_afgeleide_attributen() throws Exception {
        int volgordes = 0;
        for (List<String> regels : TmsDocumenten.permutaties(REGELS))
            for (List<String> groepen : TmsDocumenten.permutaties(GROEPEN))
                for (DroolsBackend.Branch tak : TAKKEN) {
                    DroolsBackend.Build b = DroolsBackend.build(vondstdocument(regels, groepen), tak);
                    vuurEnZetTerug(b, b.kieBase(), " op " + tak + documentvolgorde(regels, groepen));
                    volgordes++;
                }
        assertEquals(8, volgordes);
    }

    /** The same write-back in a seeded sample of the orders of the generated rules, on the exec-model branch and on
     *  {@code DRL_TMS} with the rules of its text in each order. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(vondstdocument(REGELS, GROEPEN), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261013L, 100)) {
            vuurEnZetTerug(b, TmsDocumenten.kieBase(b, volgorde), " op " + b.branch() + " in volgorde "
                    + volgorde.stream().map(Rule::getName).toList());
            volgordes++;
        }
        DroolsBackend.Build t = DroolsBackend.build(vondstdocument(REGELS, GROEPEN), DroolsBackend.Branch.DRL_TMS);
        List<String> regels = Tekstvolgorde.regels(t);
        assertEquals(gegenereerd.stream().map(Rule::getName).toList(), regels, "de regels van de tekst");
        for (List<String> volgorde : TmsDocumenten.steekproef(regels, 20261013L, 100)) {
            vuurEnZetTerug(t, Tekstvolgorde.kieBase(t, volgorde), " op " + t.branch() + " in volgorde " + volgorde);
            volgordes++;
        }
        assertEquals(2 * 102, volgordes);
    }

    /** Fires V1, V2 and V3 through {@code kieBase}, which holds the rules of {@code b}, and writes back: before the
     *  write-back the session holds the derivations and each bean its input only, after it each bean carries what the
     *  class comment says. */
    private static void vuurEnZetTerug(DroolsBackend.Build b, KieBase kieBase, String waar) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = kieBase.newKieSession();
        try {
            List<Object> v = new ArrayList<>();
            for (int i = 0; i < GETALLEN.size(); i++)
                v.add(boon(b.beanLoader(), "Vondst", GETALLEN.get(i), INGEVOERDE_KENMERKEN.get(i)));
            for (Object boon : v) sessie.insert(boon);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), "geen fixpunt" + waar);
            // the session holds the derivations, and each bean still holds its input only
            assertGetal("40", sessiewaarde(sessie, v.get(0), "heffing"), "V1 heffing in the session" + waar);
            assertGetal("15", sessiewaarde(sessie, v.get(0), "vindloon"), "V1 vindloon in the session" + waar);
            assertGetal("20", sessiewaarde(sessie, v.get(0), "uitkering"), "V1 uitkering in the session" + waar);
            assertTrue(sessie.getObjects(o -> o instanceof HeeftKenmerk h && h.onderwerp() == v.get(1)).size() == 1,
                    "V2 vrijgesteld in the session" + waar);
            assertNull(lees(v.get(0), "uitkering"), "V1 uitkering before the write-back" + waar);
            assertNull(lees(v.get(0), "vindloon"), "V1 vindloon before the write-back" + waar);
            assertGetal("7", lees(v.get(0), "heffing"), "V1 heffing before the write-back" + waar);
            assertFalse(heeft(v.get(1), "vrijgesteld"), "V2 vrijgesteld before the write-back" + waar);
            assertNull(lees(v.get(2), "uitkering"), "V3 uitkering before the write-back" + waar);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertGetal("20", lees(v.get(0), "uitkering"), "V1 uitkering" + waar);
            assertGetal("15", lees(v.get(0), "vindloon"), "V1 vindloon" + waar);
            assertGetal("40", lees(v.get(0), "heffing"), "V1 heffing" + waar);
            assertTrue(heeft(v.get(0), "vrijgesteld"), "V1 vrijgesteld, the input, stays" + waar);
            assertGetal("20", lees(v.get(1), "uitkering"), "V2 uitkering" + waar);
            assertGetal("3", lees(v.get(1), "vindloon"), "V2 vindloon, the input, stays" + waar);
            assertGetal("7", lees(v.get(1), "heffing"), "V2 heffing, the input, stays" + waar);
            assertTrue(heeft(v.get(1), "vrijgesteld"), "V2 vrijgesteld" + waar);
            assertGetal("8", lees(v.get(2), "uitkering"), "V3 uitkering" + waar);
            assertGetal("9", lees(v.get(2), "vindloon"), "V3 vindloon" + waar);
            assertNull(lees(v.get(2), "heffing"), "V3 heffing" + waar);
            assertFalse(heeft(v.get(2), "vrijgesteld"), "V3 vrijgesteld" + waar);
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Two writers of one attribute of one object with different values have no common fixpoint, and the write-back
     *  does not choose: the facts are put in the session by hand, since the branch refuses every document that could
     *  derive them (two computing rules, or two initialisations, on one target), and in both orders. */
    @Test void twee_verschillende_waarden_op_een_doel_zijn_een_benoemde_fout_in_elke_volgorde() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN) {
            List<String> zinnen = new ArrayList<>();
            Object v1 = tweeSchrijvers(tak, new BigDecimal("20"), new BigDecimal("7"), zinnen);
            Object geschreven = lees(v1, "uitkering");
            assertEquals(2, zinnen.size(), () -> tak + ": a failure in each order, got " + zinnen + ", V1 uitkering "
                    + geschreven);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel"
                    + " \"uitkering\" schrijft 20 en de regel \"uitkering volgens een tweede regel\" schrijft 7 in het"
                    + " attribuut uitkering van een Vondst (Vondst@" + Integer.toHexString(System.identityHashCode(v1))
                    + "); divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)", zinnen.get(0), tak.toString());
            assertEquals(zinnen.get(0), zinnen.get(1), tak + ": the sentence does not depend on the order of the two"
                    + " facts");
            assertNull(geschreven, tak + ": a divergence writes neither value");
        }
    }

    @Test void gelijke_waarden_van_twee_schrijvers_zijn_een_waarde() throws Exception {
        for (DroolsBackend.Branch tak : TAKKEN) {
            List<String> zinnen = new ArrayList<>();
            Object v1 = tweeSchrijvers(tak, new BigDecimal("20"), new BigDecimal("20.0"), zinnen);
            assertEquals(List.of(), zinnen, tak.toString());
            assertGetal("20", lees(v1, "uitkering"), tak + ": V1 uitkering");
        }
    }

    /** Puts a Waarde of {@code eerste} by the rule "uitkering" and one of {@code tweede} by another writer on the
     *  uitkering of one Vondst, in both orders, each in a fresh session of {@code tak}, writes back, and collects the
     *  failures. */
    private static Object tweeSchrijvers(DroolsBackend.Branch tak, BigDecimal eerste, BigDecimal tweede,
                                         List<String> zinnen) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(vondstdocument(REGELS, GROEPEN), tak);
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            Object v1 = boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            ElementSleutel uitkering = ElementSleutel.van("Vondst", "uitkering", false);
            Waarde a = new Waarde(v1, uitkering, eerste, "uitkering");
            Waarde z = new Waarde(v1, uitkering, tweede, "uitkering volgens een tweede regel");
            for (List<Waarde> volgorde : List.of(List.of(a, z), List.of(z, a))) {
                KieSession sessie = b.kieBase().newKieSession();
                try {
                    volgorde.forEach(sessie::insert);
                    // no rule reads a Waarde without its Vondst in the session, so the fire leaves the two as they are
                    assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "the fixpoint");
                    try {
                        DroolsBackend.zetAfgeleideWaarden(b, sessie);
                    } catch (IllegalStateException e) {
                        zinnen.add(e.getMessage());
                    }
                } finally {
                    sessie.dispose();
                }
            }
            return v1;
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static Path vondstdocument(List<String> regels, List<String> groepen) throws Exception {
        return TmsDocumenten.document(VONDST, regels, List.of(), groepen);
    }

    private static JsonNode vondstzaak() throws Exception {
        List<String> instanties = new ArrayList<>();
        for (int i = 0; i < GETALLEN.size(); i++)
            instanties.add(instantie("V" + (i + 1), "Vondst", GETALLEN.get(i), INGEVOERDE_KENMERKEN.get(i), Map.of()));
        return TmsDocumenten.zaak(instanties,
                List.of(waarde("V1", "uitkering", "20"), waarde("V1", "vindloon", "15"), waarde("V1", "heffing", "40"),
                        verwacht("V1", "vrijgesteld", true),
                        waarde("V2", "uitkering", "20"), waarde("V2", "vindloon", "3"), waarde("V2", "heffing", "7"),
                        verwacht("V2", "vrijgesteld", true),
                        waarde("V3", "uitkering", "8"), waarde("V3", "vindloon", "9"), waarde("V3", "heffing", null),
                        verwacht("V3", "vrijgesteld", false)));
    }

    private static String documentvolgorde(List<String> regels, List<String> groepen) throws Exception {
        return " in documentvolgorde " + TmsDocumenten.regelnamen(regels) + " " + TmsDocumenten.regelnamen(groepen);
    }

    /** The value of the one {@link Waarde} the session holds on {@code attribuut} of {@code boon}, or null. */
    private static Object sessiewaarde(KieSession sessie, Object boon, String attribuut) {
        List<Object> w = new ArrayList<>(sessie.getObjects(o -> o instanceof Waarde x && x.onderwerp() == boon
                && x.attribuut().naam().equals(attribuut)));
        assertTrue(w.size() <= 1, () -> "meer dan één Waarde op " + attribuut + ": " + w);
        return w.isEmpty() ? null : ((Waarde) w.get(0)).waarde();
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
