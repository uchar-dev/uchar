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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * at the fixpoint, with the sentence the destructive exec-model branch gave out of its fire. The document with two
 * faults: "Het aandeel … als zijn opbrengst gedeeld door zijn deelnemers", "De verhouding … als zijn taxatie gedeeld
 * door zijn deelnemers, indien hij erkend is" and the erkenning; V1 (gemeld, opbrengst 100, taxatie 30, deelnemers 0)
 * reports both, ordered by the name of the rule, in every order. Both runners faulted on these two documents in every
 * order, "division by numeric zero" (measured 2026-10-08).
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
 *
 * <p><b>The CLI's ERROR line.</b> The CLI bounded its ERROR line at 200 characters across the whole message, so on the
 * document with two faults the line broke off in the second sentence, before the rule "verhouding", while the rows above
 * compared the exception and not that line. The message now opens with a head that names the rule of every fault, and
 * the CLI bounds only the sentences after it. <b>Red before the change</b> (measured 2026-10-08 through
 * {@code DroolsBackendMain.voer}, the CLI without its exit, with the line formed as before; the jar of the branch printed
 * the same line): in each of the six orders "[noemt "verhouding" niet, opent niet met exit 3 en de kop]: 3 ERROR
 * Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten: deling door nul in de regel "aandeel": 100 gedeeld door 0 —
 * een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4); deling door…", and on the
 * document of long rule names "[noemt "de verhouding … van de gemeente" niet, noemt "het aandeel … van de gemeente"
 * niet, opent niet met exit 3 en de kop]: 3 ERROR Evaluatiefouten: aan het fixpunt staan 4 evaluatiefouten: deling door
 * nul in de regel "de verhouding van een vondst … op het strand van de g…". <b>Red by mutation</b> (not committed): with
 * the head in the message and the bound across the whole line again, the document of long rule names lost its second
 * rule in each order, "[noemt "het aandeel … van de gemeente" niet, opent niet met exit 3 en de kop]: 3 ERROR
 * Evaluatiefouten: aan het fixpunt staan 4 evaluatiefouten, in de regels "de verhouding … van de gemeente" (2×), …",
 * while the document with two faults stayed green, its head being shorter than the bound.
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
            uitkomsten.add(namen + " " + speel(tijdelijk(volgorde), tijdelijkeZaak(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(2, uitkomsten.size());
    }

    /** The two rules, the two defaults, and the standings of the bergloon and of erkend; a seeded sample of their
     *  orders, at least one of which must let the reader divide by the transient 0 of the lower default. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(tijdelijk(TIJDELIJK), DroolsBackend.Branch.EXEC_MODEL);
        List<Rule> gegenereerd = b.execModelTranspiler().model().getRules();
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
            uitkomsten.add(namen + " " + speel(blijvend(volgorde), blijvendeZaak(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " " + AANDEEL_DOOR_NUL);
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(2, uitkomsten.size());
    }

    /** The fire reaches its fixpoint without throwing, in every order of the two rules and the three standings of
     *  erkend, and the write-back reports the fault that stands there, as a consumer that fires a KieBase itself
     *  meets it. */
    @Test void de_fout_telt_aan_het_fixpunt_en_niet_in_de_fire() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(blijvend(BLIJVEND), DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.execModelTranspiler().model().getRules())) {
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

    /** The head of the two faults: their number and the rule of each, in the order of the report. */
    private static final String TWEE_KOP = "aan het fixpunt staan 2 evaluatiefouten, in de regels \"aandeel\","
            + " \"verhouding\"";

    private static final String TWEE_FOUTEN = "Evaluatiefouten: " + TWEE_KOP + ": deling door nul in de regel"
            + " \"aandeel\": 100 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS"
            + " ch. 1.4, punt 4); deling door nul in de regel \"verhouding\": 30 gedeeld door 0 — een numerieke nul als"
            + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void twee_blijvende_fouten_staan_in_elke_volgorde_in_dezelfde_vaste_volgorde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TWEE)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + speel(twee(volgorde), tweeZaak(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " " + TWEE_FOUTEN);
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(6, uitkomsten.size());
    }

    /** The three rules and the three standings of erkend: 720 orders, every one tried. */
    @Test void twee_blijvende_fouten_in_elke_volgorde_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(twee(TWEE), DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.execModelTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst",
                    Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"), List.of("gemeld"));
            assertEquals(TWEE_FOUTEN, vuur(b, volgorde, List.of(v1), new DefaultAgendaEventListener() {},
                    new Foutfeiten()), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(720, volgordes);
    }

    /** The fault a consumer meets at the fixpoint is the exception the fire of the destructive exec-model branch gave
     *  through {@code DroolsBackend.benoem}, which the DRL-text branch's fire gives still: the named division, with the
     *  division as {@code Breuk.delen} threw it as its cause, so the red path's cause chain reads as on that branch.
     *  Two faults are one exception whose cause is the first fault of its message and whose suppressed exception is the
     *  second. */
    @Test void de_fouten_aan_het_fixpunt_dragen_de_deling_als_oorzaak() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(blijvend(BLIJVEND), DroolsBackend.Branch.EXEC_MODEL);
        RuntimeException een = fixpuntfout(b, b.execModelTranspiler().model().getRules(), List.of(TmsDocumenten.boon(
                b.beanLoader(), "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"), List.of("gemeld"))),
                new DefaultAgendaEventListener() {}, new Foutfeiten());
        assertInstanceOf(Breuk.DelingDoorNul.class, een);
        assertEquals(AANDEEL_DOOR_NUL, "DelingDoorNul: " + een.getMessage());
        assertInstanceOf(Breuk.DelingDoorNul.class, een.getCause());
        assertEquals("Breuk deling door 0", een.getCause().getMessage());

        DroolsBackend.Build b2 = DroolsBackend.build(twee(TWEE), DroolsBackend.Branch.EXEC_MODEL);
        RuntimeException twee = fixpuntfout(b2, b2.execModelTranspiler().model().getRules(), List.of(TmsDocumenten.boon(
                b2.beanLoader(), "Vondst", Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"),
                List.of("gemeld"))), new DefaultAgendaEventListener() {}, new Foutfeiten());
        assertEquals(TWEE_FOUTEN, twee.getClass().getSimpleName() + ": " + twee.getMessage());
        assertEquals(AANDEEL_DOOR_NUL, "DelingDoorNul: " + twee.getCause().getMessage());
        assertEquals(1, twee.getSuppressed().length);
        assertInstanceOf(Breuk.DelingDoorNul.class, twee.getSuppressed()[0]);
        assertTrue(twee.getSuppressed()[0].getMessage().startsWith("deling door nul in de regel \"verhouding\": 30"),
                () -> "onderdrukt: " + twee.getSuppressed()[0]);
    }

    // ---------------- the CLI's status line ----------------

    /** The CLI's ERROR line on the document with two faults, in each of its six orders: exit code 3, and a line that
     *  opens with the head naming both rules and ends where the bound shortened the sentences. The line is the
     *  exception's message with its sentences bounded, and that message is pinned over the 720 orders of the generated
     *  rules by {@link #twee_blijvende_fouten_in_elke_volgorde_van_de_gegenereerde_regels}. */
    @Test void de_errorregel_van_de_cli_noemt_beide_staande_fouten_in_elke_volgorde() throws Exception {
        Path zaak = zaakbestand(tweeZaak());
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TWEE)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            String regel = cli(twee(volgorde).toString(), zaak.toString());
            List<String> mis = gebreken(regel, TWEE_KOP, List.of("aandeel", "verhouding"));
            uitkomsten.add(namen + (mis.isEmpty() ? " in orde" : " " + mis + ": " + regel));
            verwacht.add(namen + " in orde");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(6, uitkomsten.size());
    }

    static final String LANG_AANDEEL = "het aandeel van een vondst in de opbrengst van de veiling over het aantal"
            + " deelnemers aan de berging op het strand van de gemeente";
    static final String LANG_VERHOUDING = "de verhouding van een vondst tussen de taxatie door de strandvonder en het"
            + " aantal deelnemers aan de berging op het strand van de gemeente";

    /** The document with two faults under rule names of over a hundred characters each. */
    static final List<String> LANG = List.of(
            rekenregel(LANG_AANDEEL, "BEREKENING", "Vondst", "aandeel",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("deelnemers", "Vondst")), "null"),
            rekenregel(LANG_VERHOUDING, "BEREKENING", "Vondst", "verhouding",
                    binair("dividedBy", lezing("taxatie", "Vondst"), lezing("deelnemers", "Vondst")),
                    check("erkend", false)),
            ERKENNING);

    /** V1 and V2, each with a numeric zero for deelnemers: each rule faults on both, four faults in all. */
    static JsonNode tweeVondstenZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantie("V1", "Vondst", Map.of("opbrengst", "100", "taxatie", "30", "deelnemers", "0"),
                        List.of("gemeld"), Map.of()),
                instantie("V2", "Vondst", Map.of("opbrengst", "50", "taxatie", "20", "deelnemers", "0"),
                        List.of("gemeld"), Map.of())),
                List.of(consistent()));
    }

    /** The bound drops no fault where the head alone is longer than the bound: two rules of long names, each faulting
     *  on two objects, in each of the six orders of the document. The head names each rule once with the number of its
     *  faults, in the fixed order (by name: "de verhouding …" before "het aandeel …"). */
    @Test void de_grens_van_de_errorregel_laat_geen_staande_fout_weg() throws Exception {
        Path zaak = zaakbestand(tweeVondstenZaak());
        String kop = "aan het fixpunt staan 4 evaluatiefouten, in de regels \"" + LANG_VERHOUDING + "\" (2×), \""
                + LANG_AANDEEL + "\" (2×)";
        assertTrue(kop.length() > 200, () -> "de kop past binnen de grens: " + kop.length() + " tekens");
        Map<String, String> kort = Map.of(LANG_AANDEEL, "aandeel …", LANG_VERHOUDING, "verhouding …");
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(LANG)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde).stream().map(n -> kort.getOrDefault(n, n)).toList();
            String regel = cli(TmsDocumenten.document(MODEL, volgorde).toString(), zaak.toString());
            List<String> mis = gebreken(regel, kop, List.of(LANG_VERHOUDING, LANG_AANDEEL));
            uitkomsten.add(namen + (mis.isEmpty() ? " in orde" : " " + mis + ": " + regel));
            verwacht.add(namen + " in orde");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(6, uitkomsten.size());
    }

    /** What is wrong with the CLI's {@code regel} (exit code, a space, the last line it printed) on a document whose
     *  faults stand under {@code kop}: empty where the exit code is 3, the line opens with the ERROR head and
     *  {@code kop}, names every rule of {@code regels}, and the bound shortened the sentences after the head. */
    private static List<String> gebreken(String regel, String kop, List<String> regels) {
        List<String> mis = new ArrayList<>();
        for (String naam : regels)
            if (!regel.contains("\"" + naam + "\"")) mis.add("noemt \"" + naam + "\" niet");
        if (!regel.startsWith("3 ERROR Evaluatiefouten: " + kop + ": ")) mis.add("opent niet met exit 3 en de kop");
        if (!regel.endsWith("…")) mis.add("de grens kortte de zinnen niet in");
        return mis;
    }

    /** The exit code of {@code DroolsBackendMain} on {@code args}, a space, and the last line it printed: the status
     *  line gate 12 and the corpus sweep read ({@code java -jar … | tail -1}), from the CLI's own code in this JVM. */
    private static String cli(String... args) {
        ByteArrayOutputStream uit = new ByteArrayOutputStream();
        int code = DroolsBackendMain.voer(args, new PrintStream(uit, true, StandardCharsets.UTF_8));
        List<String> regels = uit.toString(StandardCharsets.UTF_8).lines().toList();
        return code + " " + (regels.isEmpty() ? "(geen uitvoer)" : regels.get(regels.size() - 1));
    }

    /** {@code zaak} in a temporary file, for the CLI. */
    private static Path zaakbestand(JsonNode zaak) throws Exception {
        Path tmp = Files.createTempFile("tms-getuige-zaak", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, zaak.toString());
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

    /** {@link #speel} on the exec-model branch and on {@code DRL_TMS}, its analysis rendered as DRL text, which gives
     *  the same outcome (card drl-teksttak-op-truth-maintenance, step 5): that outcome. */
    static String speelBeide(Path ir, JsonNode zaak) throws Exception {
        String exec = speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL);
        assertEquals(exec, speel(ir, zaak, DroolsBackend.Branch.DRL_TEXT), () -> "DRL_TMS speelt " + ir
                + " anders dan de exec-model-tak");
        return exec;
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
            DroolsBackend.Vuring vuring;
            try {
                vuring = DroolsBackend.vuur(b, sessie, 100_000);
            } catch (RuntimeException e) {
                return fail("de fire wierp in volgorde " + namen + ": " + e);
            }
            assertNull(vuring.geenFixpunt(), () -> "geen fixpunt in volgorde " + namen);
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
