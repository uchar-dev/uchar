package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.tms.Stand;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.predicaat;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * An evaluation fault in a rule's value on the truth-maintenance branch: a division by a numeric zero is a fault
 * (RUNTIME-SEMANTICS ch. 1.4, point 4), and a fault, like a value, belongs to the fixpoint (ch. 5.2). The branch has no
 * strata and passes intermediate states the reference interpreter does not see, and a fault thrown out of a consequence
 * cannot be withdrawn, so the fault is a fact inserted logically, justified by the activation that raised it, and the
 * session reports the faults that stand once it reaches its fixpoint, in an order that does not depend on the order of
 * the rules.
 *
 * <p>The transient document: a Vondst with {@code taxatie}, {@code opbrengst}, {@code bergloon} and {@code aandeel},
 * "Een Vondst is erkend indien hij gemeld is", a defaults group on the bergloon, higher "op zijn taxatie indien hij
 * erkend is" and lower "op 0 indien hij gemeld is", and the reader "Het aandeel van een Vondst moet berekend worden als
 * zijn opbrengst gedeeld door zijn bergloon, indien zijn bergloon gevuld is". V1 (gemeld, taxatie 30, opbrengst 90):
 * bergloon 30, aandeel 3, consistent. Where the lower default decides before the standing of {@code erkend} arrives, the
 * reader divides 90 by the transient 0; that fault goes when the higher default displaces the lower one. The reference
 * interpreter's runner and the ASP runner passed the case in both orders of the document's rules (measured
 * 2026-10-08). The reader's condition keeps the reference interpreter off a division by leeg: without it, that runner
 * computed the reader before the defaults group decided and kept the §6.5 deposit of 90 divided by leeg, "MISMATCH:
 * expected consistent, but the channel carries: aandeel × V1", in both orders (RUNTIME-SEMANTICS ch. 6.1, "Recorded
 * residuals"), where the ASP runner passed and this branch withdraws the deposit.
 *
 * <p>The standing document: "Het aandeel van een Vondst moet berekend worden als zijn opbrengst gedeeld door zijn
 * deelnemers, indien hij erkend is" beside the erkenning; V1 (gemeld, opbrengst 100, deelnemers 0 in the input) faults
 * at the fixpoint, with the sentence the exec-model branch gives out of its fire. The document with two faults:
 * "Het aandeel … als zijn opbrengst gedeeld door zijn deelnemers", "De verhouding … als zijn taxatie gedeeld door zijn
 * deelnemers, indien hij erkend is" and the erkenning; V1 (gemeld, opbrengst 100, taxatie 30, deelnemers 0) reports
 * both, ordered by the name of the rule, in every order. Both runners faulted on these two documents in every order,
 * "division by numeric zero" (measured 2026-10-08).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch at its claim, where a consequence threw the
 * fault): the transient document played into the fault in both orders, "[erkenning, aandeel] DelingDoorNul: deling door
 * nul in de regel "aandeel": 90 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS
 * ch. 1.4, punt 4)"; every row that fires a session itself stopped in the fire, "de fire wierp in volgorde [erkenning,
 * aandeel, kenmerkstand Vondst.erkend: waar uit de invoer, …]: Exception executing consequence for rule "aandeel" in
 * dev.uchar.regelspraak.tms: dev.uchar.regelspraak.runtime.Breuk$DelingDoorNul: Breuk deling door 0"; and the document
 * with two faults reported the first fault the agenda reached and no other, "[aandeel, verhouding, erkenning]
 * DelingDoorNul: deling door nul in de regel "aandeel": 100 gedeeld door 0 — …", in each of its six orders. The replay
 * of the single standing fault and the exec-model rows were green before: they pin what stays.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the value's fault inserted by {@code insert}
 * instead of {@code logicalInsert}, the transient fault stayed: "[erkenning, aandeel] DelingDoorNul: deling door nul in
 * de regel "aandeel": 90 gedeeld door 0 — …" in both document orders, and in the sample "expected: &lt;null&gt; but
 * was: &lt;DelingDoorNul: deling door nul in de regel "aandeel": 90 gedeeld door 0 — …&gt;". Without the sort at the
 * write-back, the two faults came in the order of the session: "but was: &lt;Evaluatiefouten: aan het fixpunt staan 2
 * evaluatiefouten: deling door nul in de regel "verhouding": 30 gedeeld door 0 — …; deling door nul in de regel
 * "aandeel": 100 gedeeld door 0 — …&gt;" in the order [aandeel, verhouding, kenmerkstand Vondst.erkend: waar uit een
 * afleiding, erkenning, …].
 */
class TmsFoutWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("taxatie", GEHEEL), attribuut("opbrengst", GEHEEL), attribuut("deelnemers", GEHEEL),
                    attribuut("bergloon", GEHEEL), attribuut("aandeel", GEHEEL), attribuut("verhouding", GEHEEL)),
            List.of("gemeld", "erkend"))), List.of());

    private static final String ERKENNING = kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false));

    // ---------------- a transient division by zero ----------------

    static final List<String> TIJDELIJK = List.of(ERKENNING,
            rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("bergloon", "Vondst")),
                    predicaat("filled", lezing("bergloon", "Vondst"))));

    static final String GROEP = attribuutgroep("bergloon van een vondst", "Vondst", "bergloon", List.of(
            standaard(check("erkend", false), lezing("taxatie", "Vondst")),
            standaard(check("gemeld", false), getal("0"))));

    static JsonNode tijdelijkeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30", "opbrengst", "90"), List.of("gemeld"), Map.of())),
                List.of(waarde("V1", "bergloon", "30"), waarde("V1", "aandeel", "3"), consistent()));
    }

    static Path tijdelijk(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(GROEP));
    }

    @Test void een_tijdelijke_deling_door_nul_verdwijnt_aan_het_fixpunt_in_elke_volgorde_van_het_document()
            throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TIJDELIJK)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + speel(tijdelijk(volgorde), tijdelijkeZaak(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(2, uitkomsten.size());
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(tijdelijk(TIJDELIJK), tijdelijkeZaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The two rules, the two defaults, and the standings of the bergloon and of erkend; a seeded sample of their
     *  orders, at least one of which must let the reader divide by the transient 0 of the lower default. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(tijdelijk(TIJDELIJK), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        int opDeTussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "opbrengst", "90"),
                    List.of("gemeld"));
            boolean[] tussenstand = {false};
            Foutfeiten feiten = new Foutfeiten();
            String fout = vuur(b, volgorde, List.of(v1), new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("aandeel")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof Stand s
                                    && s.waarde() instanceof BigDecimal d && d.signum() == 0))
                        tussenstand[0] = true;
                }
            }, feiten);
            assertNull(fout, () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertGetal("30", TmsDocumenten.lees(v1, "bergloon"), "bergloon in volgorde " + namen);
            assertGetal("3", TmsDocumenten.lees(v1, "aandeel"), "aandeel in volgorde " + namen);
            if (tussenstand[0]) {
                assertTrue(feiten.ingevoegd > 0, () -> "de deling door de tijdelijke 0 gaf geen foutfeit, volgorde " + namen);
                opDeTussenstand++;
            }
        }
        assertTrue(opDeTussenstand > 0, "geen volgorde liet de lezer delen door de tijdelijke 0 van de lagere default");
    }

    // ---------------- a fault that stands at the fixpoint ----------------

    static final List<String> BLIJVEND = List.of(ERKENNING,
            rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")),
                    check("erkend", false)));

    static JsonNode blijvendeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"), List.of("gemeld"),
                        Map.of())),
                List.of(waarde("V1", "aandeel", null), consistent()));
    }

    static Path blijvend(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels);
    }

    private static final String AANDEEL_DOOR_NUL = "DelingDoorNul: deling door nul in de regel \"aandeel\": 100 gedeeld"
            + " door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void een_blijvende_deling_door_nul_geeft_in_elke_volgorde_dezelfde_melding_als_de_exec_modeltak()
            throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(BLIJVEND)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + speel(blijvend(volgorde), blijvendeZaak(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " " + AANDEEL_DOOR_NUL);
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(2, uitkomsten.size());
        assertEquals(AANDEEL_DOOR_NUL, speel(blijvend(BLIJVEND), blijvendeZaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The fire reaches its fixpoint without throwing, in every order of the two rules and the three standings of
     *  erkend, and the write-back reports the fault that stands there, as a consumer that fires a KieBase itself
     *  meets it. */
    @Test void de_fout_telt_aan_het_fixpunt_en_niet_in_de_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(blijvend(BLIJVEND), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.tmsTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"),
                    List.of("gemeld"));
            assertEquals(AANDEEL_DOOR_NUL, vuur(b, volgorde, List.of(v1), new DefaultAgendaEventListener() {},
                    new Foutfeiten()), () -> "volgorde " + namen);
            assertNull(TmsDocumenten.lees(v1, "aandeel"), () -> "een fout schrijft niets terug, volgorde " + namen);
            volgordes++;
        }
        assertEquals(120, volgordes);
    }

    // ---------------- two faults that stand ----------------

    static final List<String> TWEE = List.of(
            rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")), "null"),
            rekenregel("verhouding", "BEREKENING", "Vondst", "verhouding",
                    binair("dividedBy", lezing("taxatie", "Vondst"), lezing("deelnemers", "Vondst")),
                    check("erkend", false)),
            ERKENNING);

    static JsonNode tweeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"),
                        List.of("gemeld"), Map.of())),
                List.of(waarde("V1", "aandeel", null), waarde("V1", "verhouding", null), consistent()));
    }

    static Path twee(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels);
    }

    private static final String TWEE_FOUTEN = "Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten: deling door nul"
            + " in de regel \"aandeel\": 100 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg"
            + " (RUNTIME-SEMANTICS ch. 1.4, punt 4); deling door nul in de regel \"verhouding\": 30 gedeeld door 0 — een"
            + " numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void twee_blijvende_fouten_staan_in_elke_volgorde_in_dezelfde_vaste_volgorde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TWEE)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + speel(twee(volgorde), tweeZaak(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " " + TWEE_FOUTEN);
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(6, uitkomsten.size());
    }

    /** The three rules and the three standings of erkend: 720 orders, every one tried. */
    @Test void twee_blijvende_fouten_in_elke_volgorde_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(twee(TWEE), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.tmsTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst",
                    Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"), List.of("gemeld"));
            assertEquals(TWEE_FOUTEN, vuur(b, volgorde, List.of(v1), new DefaultAgendaEventListener() {},
                    new Foutfeiten()), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(720, volgordes);
    }

    /** The fault a consumer meets at the fixpoint is the exception the exec-model branch's fire gives through
     *  {@code DroolsBackend.benoem}: the named division, with the division as {@code Breuk.delen} threw it as its cause,
     *  so the red path's cause chain reads as on that branch. Two faults are one exception whose cause is the first
     *  fault of its message and whose suppressed exception is the second. */
    @Test void de_fouten_aan_het_fixpunt_dragen_de_deling_als_oorzaak() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(blijvend(BLIJVEND), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        RuntimeException een = fixpuntfout(b, b.tmsTranspiler().model().getRules(), List.of(TmsDocumenten.boon(
                b.beanLoader(), "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"), List.of("gemeld"))),
                new DefaultAgendaEventListener() {}, new Foutfeiten());
        assertInstanceOf(Breuk.DelingDoorNul.class, een);
        assertEquals(AANDEEL_DOOR_NUL, "DelingDoorNul: " + een.getMessage());
        assertInstanceOf(Breuk.DelingDoorNul.class, een.getCause());
        assertEquals("Breuk deling door 0", een.getCause().getMessage());

        DroolsBackend.Build b2 = DroolsBackend.build(twee(TWEE), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        RuntimeException twee = fixpuntfout(b2, b2.tmsTranspiler().model().getRules(), List.of(TmsDocumenten.boon(
                b2.beanLoader(), "Vondst", Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"),
                List.of("gemeld"))), new DefaultAgendaEventListener() {}, new Foutfeiten());
        assertEquals(TWEE_FOUTEN, twee.getClass().getSimpleName() + ": " + twee.getMessage());
        assertEquals(AANDEEL_DOOR_NUL, "DelingDoorNul: " + twee.getCause().getMessage());
        assertEquals(1, twee.getSuppressed().length);
        assertInstanceOf(Breuk.DelingDoorNul.class, twee.getSuppressed()[0]);
        assertTrue(twee.getSuppressed()[0].getMessage().startsWith("deling door nul in de regel \"verhouding\": 30"),
                () -> "onderdrukt: " + twee.getSuppressed()[0]);
    }

    // ---------------- helpers ----------------

    /** The replay of {@code zaak} on {@code tak}: "gespeeld" where every expectation matched, the replay's verdict line
     *  where one did not, and the exception the replay raised as the CLI's ERROR line names it. */
    static String speel(Path ir, JsonNode zaak, DroolsBackend.Branch tak) throws Exception {
        try {
            String verdict = CaseRunner.speelCase(ir, zaak, tak);
            return verdict == null ? "gespeeld" : verdict;
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** {@link #fixpuntfout} as "SimpleName: message", or null where the write-back raises nothing. */
    static String vuur(DroolsBackend.Build b, List<Rule> volgorde, List<Object> bonen, AgendaEventListener agenda,
                       Foutfeiten feiten) {
        RuntimeException fout = fixpuntfout(b, volgorde, bonen, agenda, feiten);
        return fout == null ? null : fout.getClass().getSimpleName() + ": " + fout.getMessage();
    }

    /** Fires {@code bonen} through the rules of {@code b} added in {@code volgorde}, with {@code agenda} on the agenda
     *  and {@code feiten} on working memory. The fire must reach its fixpoint below the cap without throwing: an
     *  evaluation fault is a fact of the session, not an exception out of the fire. Returns the fault the write-back
     *  raises at the fixpoint, or null when it writes the derivations back. */
    static RuntimeException fixpuntfout(DroolsBackend.Build b, List<Rule> volgorde, List<Object> bonen,
                                        AgendaEventListener agenda, Foutfeiten feiten) {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            sessie.addEventListener(agenda);
            sessie.addEventListener(feiten);
            for (Object boon : bonen) sessie.insert(boon);
            int vuringen;
            try {
                vuringen = sessie.fireAllRules(100_000);
            } catch (RuntimeException e) {
                return fail("de fire wierp in volgorde " + namen + ": " + e);
            }
            assertTrue(vuringen < 100_000, () -> "geen fixpunt in volgorde " + namen);
            try {
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    /** Counts the fault facts a session inserts and withdraws, by the simple name of their class. */
    static final class Foutfeiten implements RuleRuntimeEventListener {
        int ingevoegd, ingetrokken;

        @Override public void objectInserted(ObjectInsertedEvent e) {
            if (e.getObject().getClass().getSimpleName().equals("Fout")) ingevoegd++;
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            if (e.getOldObject().getClass().getSimpleName().equals("Fout")) ingetrokken++;
        }
    }

    static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
