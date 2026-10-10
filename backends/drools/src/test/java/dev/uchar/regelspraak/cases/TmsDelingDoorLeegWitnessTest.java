package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.schema.FactModelGenerator;

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
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §6.5 deposit of a division by leeg on the truth-maintenance branch: a filled dividend over a leeg divisor
 * computes leeg and deposits an inconsistency under the rule's name on the subject (RUNTIME-SEMANTICS ch. 1.2, the
 * exec-model branch's {@code delingCompanions}), and only where the rule's condition holds. "Een Vondst is geclaimd
 * indien hij gemeld is", "Een Vondst is vrij indien hij niet geclaimd is", "Het aandeel van een Vondst moet berekend
 * worden als zijn opbrengst gedeeld door zijn deelnemers, indien hij vrij is". V1 is vrij with opbrengst 100 and no
 * deelnemers: aandeel leeg and the deposit. In the second case none: V2 is gemeld, so geclaimd and not vrij, with the
 * same opbrengst and no deelnemers; V3 is vrij without an opbrengst (leeg divided by leeg is 0); V4 is vrij with
 * opbrengst 100 and 4 deelnemers (aandeel 25). The deposit is a logical fact, so in an order where the aandeel rule
 * fires on V2 before geclaimd is derived, the deposit made on the transient vrij goes when geclaimd arrives; the
 * reference interpreter keeps a deposit made on a transient value (RUNTIME-SEMANTICS ch. 6.1, "Recorded residuals"),
 * but a stratified program gives it none here. Every order of the three rules in the document, and a seeded sample of
 * orders of the generated rules, at least one of which must fire the aandeel rule on V2.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step): both rows on this branch were
 * refused, "tms-transpiler SKIP: regel: aandeel — een attribuutdoel (berekening van 'aandeel' van Vondst): de
 * truth-maintenance-tak leidt nog alleen kenmerken af". The row on the exec-model branch was green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the deposit inserted as a stated fact instead
 * of a logical one, the deposit made on the transient path stayed, and the sample failed on "inconsistenties in volgorde
 * [kenmerkstand Vondst.vrij: onwaar, …, aandeel, vrijgave, claim] ==&gt; expected: &lt;[]&gt; but was:
 * &lt;[Inconsistentie[aandeel @ dev.uchar.regelspraak.facts.Vondst@…]]&gt;". The document orders stayed green under that
 * mutant: in none of them does the replay fire the aandeel rule on V2, which is why the sample row is here.
 */
class TmsDelingDoorLeegWitnessTest {

    private static final List<String> REGELS = List.of(
            kenmerkregel("claim", "Vondst", "geclaimd", check("gemeld", false)),
            kenmerkregel("vrijgave", "Vondst", "vrij", check("geclaimd", true)),
            rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")),
                    check("vrij", false)));

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("opbrengst", GEHEEL), attribuut("deelnemers", GEHEEL), attribuut("aandeel", GEHEEL)),
            List.of("gemeld", "geclaimd", "vrij"))), List.of());

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = namen(volgorde);
            for (JsonNode zaak : List.of(metDeposito(), zonderDeposito()))
                assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, volgorde), zaak,
                        DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    /** Three rules and the characteristic standings of geclaimd and vrij (three rules each). */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, REGELS),
                DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
        assertEquals(9, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int doorDeTussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100))
            if (vuur(b, volgorde)) doorDeTussenstand++;
        assertTrue(doorDeTussenstand > 0, "geen volgorde vuurde de aandeelregel op V2");
    }

    /** Fires V2, V3 and V4 with the rules in {@code volgorde}, checks that no inconsistency stands at the fixpoint and
     *  the beans after the write-back, and says whether the aandeel rule fired on V2, whose vrij is onwaar at the end:
     *  that firing deposited on the transient path. */
    private static boolean vuur(DroolsBackend.Build b, List<Rule> volgorde) throws Exception {
        List<String> namen = volgorde.stream().map(Rule::getName).toList();
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        KieSession sessie = TmsDocumenten.kieBase(b, volgorde).newKieSession();
        try {
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("opbrengst", "100"), List.of("gemeld"));
            Object v3 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of());
            Object v4 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("opbrengst", "100", "deelnemers", "4"),
                    List.of());
            boolean[] tussenstand = {false};
            sessie.addEventListener(new DefaultAgendaEventListener() {
                @Override public void afterMatchFired(AfterMatchFiredEvent e) {
                    if (e.getMatch().getRule().getName().equals("aandeel")
                            && e.getMatch().getObjects().stream().anyMatch(o -> o == v2))
                        tussenstand[0] = true;
                }
            });
            sessie.insert(v2);
            sessie.insert(v3);
            sessie.insert(v4);
            assertNull(DroolsBackend.vuur(b, sessie, 100_000).geenFixpunt(), () -> "geen fixpunt in volgorde " + namen);
            String waar = " in volgorde " + namen;
            String inconsistentie = FactModelGenerator.PAKKET + ".Inconsistentie";
            assertEquals(List.of(), List.copyOf(sessie.getObjects(o -> o.getClass().getName().equals(inconsistentie))),
                    "inconsistenties" + waar);
            DroolsBackend.zetAfgeleideWaarden(b, sessie);
            assertEquals(List.of(true, false), List.of(TmsDocumenten.heeft(v2, "geclaimd"), TmsDocumenten.heeft(v2, "vrij")),
                    "V2" + waar);
            assertNull(TmsDocumenten.lees(v2, "aandeel"), "V2 aandeel" + waar);
            assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) TmsDocumenten.lees(v3, "aandeel")), "V3 aandeel" + waar);
            assertEquals(0, new BigDecimal("25").compareTo((BigDecimal) TmsDocumenten.lees(v4, "aandeel")), "V4 aandeel" + waar);
            return tussenstand[0];
        } finally {
            sessie.dispose();
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static JsonNode metDeposito() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("opbrengst", "100"), List.of(), Map.of())),
                List.of(verwacht("V1", "vrij", true), waarde("V1", "aandeel", null), inconsistent("aandeel", "V1")));
    }

    private static JsonNode zonderDeposito() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V2", "Vondst", Map.of("opbrengst", "100"), List.of("gemeld"), Map.of()),
                        instantie("V3", "Vondst", Map.of(), List.of(), Map.of()),
                        instantie("V4", "Vondst", Map.of("opbrengst", "100", "deelnemers", "4"), List.of(), Map.of())),
                List.of(consistent(), verwacht("V2", "vrij", false), waarde("V2", "aandeel", null),
                        verwacht("V3", "vrij", true), waarde("V3", "aandeel", "0"),
                        verwacht("V4", "vrij", true), waarde("V4", "aandeel", "25")));
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
