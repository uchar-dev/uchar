package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Stand;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Initialisations on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 5.4: the guard reads the input, not the
 * initialisation's own write). The first document has the shape of the golden {@code jutterswet-initialisatievolgorde},
 * on whole numbers: "Het bergloon van een Vondst moet geïnitialiseerd worden op 2 maal zijn berekend gewicht indien zijn
 * gewicht groter is dan 10", "Het vindloon van een Vondst moet geïnitialiseerd worden op zijn berekend gewicht plus 5"
 * and "Het berekend gewicht van een Vondst moet berekend worden als zijn gewicht plus 1". V1 (gewicht 12): bergloon 26,
 * vindloon 18. V2 (gewicht 8): bergloon leeg, vindloon 14. V3 (gewicht 12, bergloon 40 and vindloon 3 as input): both
 * inputs stand. V4 (gewicht 30, vindloon 7 as input): bergloon 62, vindloon 7. An initialisation that fires before the
 * berekend gewicht is derived reads its input standing, leeg; its own write does not block it, so truth maintenance
 * withdraws that write when the standing changes and the initialisation fires again on the derived value. Every order of
 * the three rules in the document, the exec-model branch on the document, and every order of the five generated rules,
 * at least one of which must fire an initialisation on that input standing.
 *
 * <p>The second document has a computed value displace an initialisation on the same target: "Een Vondst is
 * geregistreerd indien hij gemeld is", "Een Vondst is onbeheerd indien hij niet geregistreerd is", "Het bergloon van een
 * Vondst moet gesteld worden op zijn taxatie, indien hij onbeheerd is" and "Het bergloon van een Vondst moet
 * geïnitialiseerd worden op 7". V1 (gemeld, taxatie 30): 7. V2 (taxatie 30): 30, whichever fires first. V3 (nothing):
 * the computation is leeg and writes nothing, so the initialisation fills the target (ch. 7.3), 7. V4 (gemeld, bergloon 5
 * as input): 5. V5 (taxatie 30, bergloon 5 as input): the computation decides and replaces the input (ch. 7.8), 30.
 * Every order of the four rules in the document, the exec-model branch on the document, and a seeded sample of orders
 * of the generated rules, at least one of which must fire the initialisation on V2 before the computation displaces it.
 * A division by leeg in an initialisation's value deposits only where the initialisation applies, and two
 * initialisations on one target are refused by name (IR-SPEC WF-079). The reference interpreter's runner passed the
 * cases of the first two documents as well (measured 2026-10-08).
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's second step, whose manifest refused every
 * initialisation): every row on this branch was refused, "tms-transpiler SKIP: regel: standaardbergloon — een
 * initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet", and the refusal row
 * got that sentence instead of its own, "expected: &lt;tms-transpiler SKIP: regel: standaardbergloon — twee of meer
 * initialisaties op één doel …&gt; but was: &lt;tms-transpiler SKIP: regel: standaardbergloon — een initialisatie (van
 * 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet&gt;". The rows on the exec-model branch
 * were green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). (1) The naive guard, no {@code Waarde} of any
 * writer on the target, so the initialisation's own write counts: the replay reached the fire cap in the document
 * orders, "volgorde [standaardbergloon, standaardvindloon, berekend gewicht van een vondst] ==&gt; expected: &lt;null&gt;
 * but was: &lt;case: geen fixpunt (kringverwijzing?)&gt;", in the displacement document, "volgorde [registratie,
 * onbeheerd, bergloon, standaardbergloon] ==&gt; expected: &lt;null&gt; but was: &lt;case: geen fixpunt
 * (kringverwijzing?)&gt;", and in the first generated order, "geen fixpunt in volgorde [standaardbergloon,
 * standaardvindloon, berekend gewicht van een vondst, stand Vondst.berekend gewicht: uit een afleiding, stand
 * Vondst.berekend gewicht: uit de invoer]"; on the single Vondst of the golden's first case both targets ended leeg
 * without a message, as the trial's A5 measured, "case1-beide-geschreven.json: attribuut bergloon: verwacht 26.00,
 * werkelijk leeg; attribuut vindloon: verwacht 18.00, werkelijk leeg" ({@link TmsProefsetWitnessTest}). (2) Without
 * the guard's read of the input on the bean: "volgorde [standaardbergloon, standaardvindloon, berekend gewicht van een
 * vondst] ==&gt; expected: &lt;null&gt; but was: &lt;case: attribuut bergloon: verwacht 40, werkelijk 26; attribuut
 * vindloon: verwacht 3, werkelijk 18; attribuut vindloon: verwacht 7, werkelijk 36&gt;", and the deposit row, "V2
 * ==&gt; expected: &lt;null&gt; but was: &lt;case: verwacht consistent, werkelijk inconsistent (standaardaandeel @
 * V2)&gt;". (3) Without the other writers' {@code Waarde} in the guard, only the displacement rows failed, at the
 * write-back: "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel "bergloon" schrijft 30
 * en de regel "standaardbergloon" schrijft 7 in het attribuut bergloon van een Vondst (Vondst@…); divergentie is een
 * fout (RUNTIME-SEMANTICS ch. 5.7)".
 */
class TmsInitialisatieWitnessTest {

    private static final String BEREKEND = lezing("berekend gewicht", "Vondst");

    private static final List<String> VOLGORDE = List.of(
            rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", binair("times", getal("2"), BEREKEND),
                    vergelijking("greater", lezing("gewicht", "Vondst"), getal("10"))),
            rekenregel("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon", binair("plus", BEREKEND, getal("5")),
                    "null"),
            rekenregel("berekend gewicht van een vondst", "BEREKENING", "Vondst", "berekend gewicht",
                    binair("plus", lezing("gewicht", "Vondst"), getal("1")), "null"));

    private static final String VOLGORDE_MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("gewicht", GEHEEL), attribuut("berekend gewicht", GEHEEL), attribuut("bergloon", GEHEEL),
                    attribuut("vindloon", GEHEEL)), List.of())), List.of());

    private static final List<String> VERDRINGING = List.of(
            kenmerkregel("registratie", "Vondst", "geregistreerd", check("gemeld", false)),
            kenmerkregel("onbeheerd", "Vondst", "onbeheerd", check("geregistreerd", true)),
            rekenregel("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", lezing("taxatie", "Vondst"),
                    check("onbeheerd", false)),
            rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", getal("7"), "null"));

    private static final String VERDRINGING_MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("taxatie", GEHEEL), attribuut("bergloon", GEHEEL)),
            List.of("gemeld", "geregistreerd", "onbeheerd"))), List.of());

    // ---------------- the shape of jutterswet-initialisatievolgorde ----------------

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(VOLGORDE)) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(VOLGORDE_MODEL, volgorde), volgordeZaak(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(TmsDocumenten.document(VOLGORDE_MODEL, VOLGORDE), volgordeZaak(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** Three rules and the two standing rules of the berekend gewicht, which both initialisations read; gewicht is
     *  input and read off the bean. Five rules have 120 orders, so every one is tried. */
    @Test void elke_volgorde_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(VOLGORDE_MODEL, VOLGORDE),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(5, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int volgordes = 0, opDeInvoerstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(gegenereerd)) {
            if (vuurVolgorde(b, volgorde)) opDeInvoerstand++;
            volgordes++;
        }
        assertEquals(120, volgordes);
        assertTrue(opDeInvoerstand > 0, "geen volgorde vuurde een initialisatie op de invoerstand van V1");
    }

    /** Fires V1 to V4 with the rules in {@code volgorde}, checks the beans after the write-back, and says whether an
     *  initialisation fired on V1 while the berekend gewicht stood at its input, leeg: the transient path. */
    private static boolean vuurVolgorde(DroolsBackend.Build b, List<Rule> volgorde) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "12"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "8"), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst",
                    Map.of("gewicht", "12", "bergloon", "40", "vindloon", "3"), List.of());
            Object v4 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "30", "vindloon", "7"), List.of());
            boolean[] tussenstand = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    List<?> feiten = e.getMatch().getObjects();
                    if (e.getMatch().getRule().getName().startsWith("standaard") && feiten.stream().anyMatch(o -> o == v1)
                            && feiten.stream().anyMatch(o -> o instanceof Stand s && s.waarde() == null))
                        tussenstand[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3, v4)) sessie.insert(v);
            assertTrue(sessie.fireAllRules(100_000) < 100_000, () -> "geen fixpunt in volgorde " + namen);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            String waar = " in volgorde " + namen;
            eindigtOp(v1, "berekend gewicht", "13", "V1" + waar);
            eindigtOp(v1, "bergloon", "26", "V1" + waar);
            eindigtOp(v1, "vindloon", "18", "V1" + waar);
            eindigtOp(v2, "bergloon", null, "V2" + waar);
            eindigtOp(v2, "vindloon", "14", "V2" + waar);
            eindigtOp(v3, "bergloon", "40", "V3" + waar);
            eindigtOp(v3, "vindloon", "3", "V3" + waar);
            eindigtOp(v4, "bergloon", "62", "V4" + waar);
            eindigtOp(v4, "vindloon", "7", "V4" + waar);
            return tussenstand[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static JsonNode volgordeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("gewicht", "8"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of("gewicht", "12", "bergloon", "40", "vindloon", "3"), List.of(),
                                Map.of()),
                        instantie("V4", "Vondst", Map.of("gewicht", "30", "vindloon", "7"), List.of(), Map.of())),
                List.of(waarde("V1", "berekend gewicht", "13"), waarde("V1", "bergloon", "26"),
                        waarde("V1", "vindloon", "18"), waarde("V2", "berekend gewicht", "9"),
                        waarde("V2", "bergloon", null), waarde("V2", "vindloon", "14"), waarde("V3", "bergloon", "40"),
                        waarde("V3", "vindloon", "3"), waarde("V4", "bergloon", "62"), waarde("V4", "vindloon", "7"),
                        consistent()));
    }

    // ---------------- a computed value displaces the initialisation ----------------

    @Test void een_berekende_waarde_verdringt_de_initialisatie_in_elke_volgorde_van_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(VERDRINGING)) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(VERDRINGING_MODEL, volgorde), verdringingZaak(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    @Test void de_exec_modeltak_geeft_dezelfde_verdringing() throws Exception {
        assertNull(CaseRunner.speelCase(TmsDocumenten.document(VERDRINGING_MODEL, VERDRINGING), verdringingZaak(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** Four rules and the characteristic standings of geregistreerd and onbeheerd (three rules each); taxatie is input
     *  and nothing reads the bergloon, so it has no standing. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels_bij_verdringing() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(VERDRINGING_MODEL, VERDRINGING),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(10, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int verdrongen = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100))
            if (vuurVerdringing(b, volgorde)) verdrongen++;
        assertTrue(verdrongen > 0, "geen volgorde vuurde de initialisatie op V2 voordat de berekening haar verdrong");
    }

    /** Fires V1 to V5 with the rules in {@code volgorde}, checks the beans after the write-back, and says whether the
     *  initialisation fired on V2, whose bergloon the computation decides: that write was displaced. */
    private static boolean vuurVerdringing(DroolsBackend.Build b, List<Rule> volgorde) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of("gemeld"));
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30"), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            Object v4 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("bergloon", "5"), List.of("gemeld"));
            Object v5 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "bergloon", "5"), List.of());
            boolean[] verdrongen = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("standaardbergloon")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v2))
                        verdrongen[0] = true;
                }
            });
            for (Object v : List.of(v1, v2, v3, v4, v5)) sessie.insert(v);
            assertTrue(sessie.fireAllRules(100_000) < 100_000, () -> "geen fixpunt in volgorde " + namen);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            String waar = " in volgorde " + namen;
            eindigtOp(v1, "bergloon", "7", "V1" + waar);
            eindigtOp(v2, "bergloon", "30", "V2" + waar);
            eindigtOp(v3, "bergloon", "7", "V3" + waar);
            eindigtOp(v4, "bergloon", "5", "V4" + waar);
            eindigtOp(v5, "bergloon", "30", "V5" + waar);
            return verdrongen[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static JsonNode verdringingZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30"), List.of("gemeld"), Map.of()),
                        instantie("V2", "Vondst", Map.of("taxatie", "30"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of(), List.of(), Map.of()),
                        instantie("V4", "Vondst", Map.of("bergloon", "5"), List.of("gemeld"), Map.of()),
                        instantie("V5", "Vondst", Map.of("taxatie", "30", "bergloon", "5"), List.of(), Map.of())),
                List.of(waarde("V1", "bergloon", "7"), waarde("V2", "bergloon", "30"), waarde("V3", "bergloon", "7"),
                        waarde("V4", "bergloon", "5"), waarde("V5", "bergloon", "30"), consistent()));
    }

    // ---------------- the §6.5 deposit under the guard ----------------

    /** "Het aandeel van een Vondst moet geïnitialiseerd worden op zijn opbrengst gedeeld door zijn deelnemers". V1
     *  (opbrengst 100, no deelnemers): aandeel leeg and the deposit, under the initialisation's name. V2 (the same,
     *  with aandeel 5 as input): the initialisation does not apply, so it evaluates nothing and deposits nothing; the
     *  reference interpreter tests the guard before it evaluates the value, and its runner passed both cases on this
     *  document (measured 2026-10-08). The exec-model branch passes V1 but deposited on V2 as well, "case: verwacht
     *  consistent, werkelijk inconsistent (standaardaandeel @ V2)" (measured the same day), so only V1 is asked of it
     *  here; recorded as an observation, since this card does not touch that branch. */
    @Test void een_deling_door_leeg_deponeert_alleen_waar_de_initialisatie_geldt() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
                List.of(attribuut("opbrengst", GEHEEL), attribuut("deelnemers", GEHEEL), attribuut("aandeel", GEHEEL)),
                List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of(rekenregel("standaardaandeel", "INITIALISATIE", "Vondst",
                "aandeel", binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")), "null")));
        JsonNode geldt = TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("opbrengst", "100"), List.of(), Map.of())),
                List.of(waarde("V1", "aandeel", null), TmsDocumenten.inconsistent("standaardaandeel", "V1")));
        JsonNode invoer = TmsDocumenten.zaak(
                List.of(instantie("V2", "Vondst", Map.of("opbrengst", "100", "aandeel", "5"), List.of(), Map.of())),
                List.of(waarde("V2", "aandeel", "5"), consistent()));
        assertNull(CaseRunner.speelCase(ir, geldt, DroolsBackend.Branch.TRUTH_MAINTENANCE), "V1");
        assertNull(CaseRunner.speelCase(ir, invoer, DroolsBackend.Branch.TRUTH_MAINTENANCE), "V2");
        assertNull(CaseRunner.speelCase(ir, geldt, DroolsBackend.Branch.EXEC_MODEL), "V1 in de exec-modeltak");
    }

    // ---------------- two initialisations on one target ----------------

    /** Where both apply nothing ranks them, conditional or not (RUNTIME-SEMANTICS ch. 5.4). The backend runs no
     *  verifier, so the branch refuses the pair itself, at the first of them in the document. */
    @Test void twee_initialisaties_op_een_doel_worden_geweigerd() throws Exception {
        String model = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
                List.of(attribuut("gewicht", GEHEEL), attribuut("bergloon", GEHEEL)), List.of())), List.of());
        Path ir = TmsDocumenten.document(model, List.of(
                rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", getal("7"), "null"),
                rekenregel("zwaar bergloon", "INITIALISATIE", "Vondst", "bergloon", getal("9"),
                        vergelijking("greater", lezing("gewicht", "Vondst"), getal("100")))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: standaardbergloon — twee of meer initialisaties op één doel ('bergloon'"
                + " van Vondst: \"standaardbergloon\", \"zwaar bergloon\"), voorwaardelijk of niet: waar ze allebei gelden"
                + " rangschikt niets ze en bepaalt het fixpunt de waarde niet (RUNTIME-SEMANTICS ch. 5.4, WF-079)",
                e.getMessage());
    }

    // ---------------- a division in an initialisation beside another writer ----------------

    private static final String DELING_MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("taxatie", GEHEEL), attribuut("bergloon", GEHEEL), attribuut("opbrengst", GEHEEL),
                    attribuut("deelnemers", GEHEEL)),
            List.of("gemeld", "erkend"))), List.of());

    private static final List<String> DELING = List.of(
            kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false)),
            rekenregel("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", lezing("taxatie", "Vondst"),
                    check("erkend", false)),
            rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")), "null"));

    private static final String DELING_WEIGERING = "tms-transpiler SKIP: regel: standaardbergloon — een deling in een"
            + " initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'bergloon' van Vondst wordt ook"
            + " geschreven door \"bergloon\"): naast een andere schrijver is een initialisatie de laagste default"
            + " (RUNTIME-SEMANTICS ch. 5.4, 7.7), en deze tak rekent haar uit zodra wat zij leest staat, ook waar de"
            + " andere schrijver haar later verdringt; een deling door nul daar is een fout op een tussenstand, die deze"
            + " tak intrekt zodra de andere schrijver beslist, waar de referentie afhankelijk van de volgorde van de"
            + " regels een fout of de waarde geeft: of zo'n fout telt, wacht op een normbesluit (ch. 5.2, 7.1)";

    /**
     * "Een Vondst is erkend indien hij gemeld is", "Het bergloon van een Vondst moet gesteld worden op zijn taxatie,
     * indien hij erkend is" and "Het bergloon van een Vondst moet geïnitialiseerd worden op zijn opbrengst gedeeld door
     * zijn deelnemers". Beside the computation the initialisation is the lowest default (RUNTIME-SEMANTICS ch. 7.7), and
     * only the deciding default is evaluated (ch. 7.1 and 7.5): V1 (gemeld, taxatie 30, opbrengst 100, deelnemers 0) ends
     * at 30, consistent, and the division by zero is never reached. This branch evaluates the initialisation as soon as
     * what it reads stands, before the computation's condition holds through the derived {@code erkend}, so it refuses
     * the division there, in every order of the document. An initialisation with a division and no other writer is not
     * speculative and stays carried ({@link #een_deling_door_leeg_deponeert_alleen_waar_de_initialisatie_geldt}).
     *
     * <p><b>Red before the refusal</b> (measured 2026-10-08, the division carried): every order of the document played
     * into the fault, the first "[erkenning, bergloon, standaardbergloon] DelingDoorNul: deling door nul in de regel
     * "standaardbergloon": 100 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS
     * ch. 1.4, punt 4)", and the other five the same.
     *
     * <p>Since the evaluation fault is a fact that counts at the fixpoint ({@link TmsFoutWitnessTest}), the fault there is
     * withdrawn when the computation displaces the initialisation, and with the refusal switched off every order played
     * bergloon 30 (measured 2026-10-08, not committed). The reference interpreter gives that value in one order of the
     * rules and the fault in the other, so whether a fault on an intermediate state counts waits on a decision of the
     * norm, and the refusal stays until then; its reason says so.
     */
    @Test void een_deling_in_een_initialisatie_naast_een_andere_schrijver_wordt_geweigerd() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30", "opbrengst", "100", "deelnemers", "0"),
                        List.of("gemeld"), Map.of())),
                List.of(waarde("V1", "bergloon", "30"), consistent()));
        List<String> verwacht = new ArrayList<>(), uitkomsten = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(DELING)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            String uitkomst;
            try {
                uitkomst = "gespeeld: " + CaseRunner.speelCase(TmsDocumenten.document(DELING_MODEL, volgorde), zaak,
                        DroolsBackend.Branch.TRUTH_MAINTENANCE);
            } catch (RuntimeException e) {
                uitkomst = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            verwacht.add(namen + " Excluded: " + DELING_WEIGERING);
            uitkomsten.add(namen + " " + uitkomst);
        }
        assertEquals(6, uitkomsten.size());
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- helpers ----------------

    /** Attribute {@code attribuut} of the bean ends at the number {@code lexeem}, or leeg where it is null. */
    private static void eindigtOp(Object boon, String attribuut, String lexeem, String waar) throws Exception {
        Object v = TmsDocumenten.lees(boon, attribuut);
        if (lexeem == null) assertNull(v, attribuut + " van " + waar);
        else assertTrue(v instanceof BigDecimal d && new BigDecimal(lexeem).compareTo(d) == 0,
                () -> attribuut + " van " + waar + ": verwacht " + lexeem + ", werkelijk " + v);
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
