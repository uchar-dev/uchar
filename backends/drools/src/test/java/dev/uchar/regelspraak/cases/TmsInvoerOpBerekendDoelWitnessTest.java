package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Stand;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.predicaat;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Case input on an attribute that a rule computes, on the truth-maintenance branch: "Het bergloon van een Vondst moet
 * gesteld worden op zijn taxatie" and "De uitkering van een Vondst moet berekend worden als zijn bergloon plus 5". The
 * rule is the bergloon's only writer, so the standing norm of RUNTIME-SEMANTICS ch. 7.8 holds: where the rule decides,
 * its value replaces the input; where it computes leeg, it writes nothing and the input stands. V1 has taxatie 30 and
 * bergloon 7 as input: bergloon 30, uitkering 35. V2 has no taxatie and bergloon 7: bergloon 7, uitkering 12. V3 has
 * neither: bergloon leeg, uitkering 5. The uitkering reads the bergloon through its standing, which carries the
 * derived value where there is one and the input otherwise; when the derived value arrives after the uitkering was
 * computed from the input, truth maintenance retracts the input standing and the uitkering computed from it. Both
 * orders of the two rules in the document, the exec-model branch on the same document, and a seeded sample of orders of
 * the generated rules, at least one of which must compute V1's uitkering from the input first. Off the standing domain
 * the rule writes a computed leeg (ch. 1.2g), which empties the target, input included.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step): both rows on this branch were
 * refused, "tms-transpiler SKIP: regel: bergloon — een attribuutdoel (gelijkstelling van 'bergloon' van Vondst): de
 * truth-maintenance-tak leidt nog alleen kenmerken af". The row on the exec-model branch was green from the start.
 * Without the refusal off the standing domain (measured the same day, on the lowering), the refusal row got the
 * sentence of the initialisation instead: "but was: &lt;tms-transpiler SKIP: regel: standaardbergloon — een
 * initialisatie (van 'bergloon' van Vondst): de truth-maintenance-tak draagt deze vorm nog niet&gt;".
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the input standing derived without its
 * {@code not Waarde}, V1 kept the input standing beside the derived one, the uitkering was computed from both, and the
 * write-back failed both rows: "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel
 * "uitkering" schrijft 12 en de regel "uitkering" schrijft 35 in het attribuut uitkering van een Vondst (Vondst@…);
 * divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)".
 */
class TmsInvoerOpBerekendDoelWitnessTest {

    private static final List<String> REGELS = List.of(
            rekenregel("bergloon", "GELIJKSTELLING", "Vondst", "bergloon", lezing("taxatie", "Vondst"), "null"),
            rekenregel("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binair("plus", lezing("bergloon", "Vondst"), getal("5")), "null"));

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("taxatie", GEHEEL), attribuut("bergloon", GEHEEL), attribuut("uitkering", GEHEEL)),
            List.of())), List.of());

    @Test void beide_volgordes_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, volgorde), zaak(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(2, volgordes);
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, REGELS), zaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    /** "Het bergloon van een Vondst moet geïnitialiseerd worden op 0, indien zijn taxatie gevuld is" beside the bergloon
     *  rule: a conditional initialisation is neither the plain one of RUNTIME-SEMANTICS ch. 7.7 nor absent, so the
     *  target is off the standing domain, where a computed leeg empties the target instead of leaving the input (ch.
     *  1.2g). Until step 3 of card tms-expressievormen-en-rekendatum the branch refused the rule there, since it wrote
     *  nothing for a computed leeg; it now writes the leeg ({@code TmsSchrijversWitnessTest}). V1 (taxatie 30, bergloon
     *  7 as input): 30, uitkering 35. V2 (bergloon 7 as input): the rule writes leeg over the input and the
     *  initialisation's condition does not hold, so bergloon leeg and uitkering 5. Every order of the three rules. The
     *  reference interpreter answers the same on this document (measured 2026-10-09, in the report of that step). */
    @Test void buiten_het_staande_domein_schrijft_de_rekenende_regel_leeg() throws Exception {
        String initialisatie = rekenregel("standaardbergloon", "INITIALISATIE", "Vondst", "bergloon", getal("0"),
                predicaat("filled", lezing("taxatie", "Vondst")));
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("bergloon", "7"), List.of(), Map.of())),
                List.of(waarde("V1", "bergloon", "30"), waarde("V1", "uitkering", "35"), waarde("V2", "bergloon", null),
                        waarde("V2", "uitkering", "5")));
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(REGELS.get(0), REGELS.get(1), initialisatie))) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, volgorde), zaak,
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    /** Two rules and the two standing rules of bergloon; taxatie is input and read off the bean. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, REGELS),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(4, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int uitDeInvoer = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 40))
            if (vuur(b, volgorde)) uitDeInvoer++;
        assertTrue(uitDeInvoer > 0, "geen volgorde berekende de uitkering van V1 uit de invoer");
    }

    /** Fires V1, V2 and V3 with the rules in {@code volgorde}, writes back, checks the beans, and says whether the
     *  uitkering rule fired on V1 over the input standing of its bergloon, which the derived value replaces. */
    private static boolean vuur(DroolsBackend.Build b, List<Rule> volgorde) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("bergloon", "7"), List.of());
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            boolean[] uitDeInvoer = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("uitkering")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o instanceof Stand s && s.onderwerp() == v1
                                    && new BigDecimal("7").equals(s.waarde())))
                        uitDeInvoer[0] = true;
                }
            });
            sessie.insert(v1);
            sessie.insert(v2);
            sessie.insert(v3);
            assertTrue(sessie.fireAllRules(100_000) < 100_000, () -> "geen fixpunt in volgorde " + namen);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            String waar = " in volgorde " + namen;
            assertGetal("30", TmsDocumenten.lees(v1, "bergloon"), "V1 bergloon" + waar);
            assertGetal("35", TmsDocumenten.lees(v1, "uitkering"), "V1 uitkering" + waar);
            assertGetal("7", TmsDocumenten.lees(v2, "bergloon"), "V2 bergloon" + waar);
            assertGetal("12", TmsDocumenten.lees(v2, "uitkering"), "V2 uitkering" + waar);
            assertNull(TmsDocumenten.lees(v3, "bergloon"), "V3 bergloon" + waar);
            assertGetal("5", TmsDocumenten.lees(v3, "uitkering"), "V3 uitkering" + waar);
            return uitDeInvoer[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }

    private static JsonNode zaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30", "bergloon", "7"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("bergloon", "7"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(waarde("V1", "bergloon", "30"), waarde("V1", "uitkering", "35"),
                        waarde("V2", "bergloon", "7"), waarde("V2", "uitkering", "12"),
                        waarde("V3", "bergloon", null), waarde("V3", "uitkering", "5")));
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
