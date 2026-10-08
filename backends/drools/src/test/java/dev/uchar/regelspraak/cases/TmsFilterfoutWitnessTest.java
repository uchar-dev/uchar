package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fault in a fold's filter on the truth-maintenance branch is the fault of the form whose evaluation reaches the
 * fold. The reference interpreter evaluates a rule's condition first and its value only where the rule fires, a
 * composite condition only up to the member after which its verdict stands (RUNTIME-SEMANTICS ch. 5.10), and a fold's
 * filter per member only where it evaluates the fold; a filter that divides by a numeric zero there faults the
 * evaluation (ch. 1.4, point 4). Nowhere else does that division happen, so nowhere else is there a fault.
 *
 * <p>The model: a Jutter reports Vondsten (aanmelding: aanmelder, one Jutter; vondst, many Vondsten); a Vondst has
 * gewicht and deelnemers, a Jutter the characteristics actief and druk. The fold "het aantal vondsten van de Jutter die
 * aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2". J1 reports V1
 * (gewicht 10, deelnemers 2: 5, heavy) and V2 (gewicht 9, deelnemers 0: the filter divides 9 by 0).
 *
 * <ul>
 *   <li>The lens's scenario: "De zware vondsten van een Jutter moet berekend worden als dat aantal, indien hij actief
 *       is", and J1 is not actief. The rule does not fire, the fold is not evaluated: no fault, zware vondsten leeg.</li>
 *   <li>The same document with J1 actief: the fault the branch reported before, named after the rule.</li>
 *   <li>"Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet: hij is actief; dat aantal is groter dan
 *       0": with J1 not actief the composite stops after its first member and never reads the fold, so no fault and
 *       J1 is not druk; with J1 actief the fault, named after that rule. A fold in a branch that is not taken is this
 *       branch's only such shape: an IfThenElse it refuses, and every operand of a Binary, a Comparison and a
 *       Predicate is evaluated, by the reference interpreter as here.</li>
 *   <li>A filter that faults on the way only: deelnemers from a defaults group, higher "op 3 indien hij erkend is",
 *       lower "op 0 indien hij gemeld is", and "Een Vondst is erkend indien hij gemeld is"; the filter guards its
 *       division with "zijn deelnemers is gevuld". Where the lower default decides first the filter divides by the
 *       transient 0; at the fixpoint V1 (gewicht 12) and V2 (gewicht 9) are both heavy, 2, and nothing faults.</li>
 * </ul>
 *
 * <p>The reference interpreter's runner and the ASP runner passed both documents where J1 is not actief, faulted on
 * both where J1 is actief ("division by numeric zero"), and passed the last document in both orders (measured
 * 2026-10-08). The exec-model branch and the DRL-text branch fault where J1 is not actief as well, the exec-model branch
 * with this class's sentence and the DRL-text branch in a constraint of its accumulate: that is theirs, and not this
 * class's business.
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch after its second step, where the filter's fault
 * rule inserted the fault on the fold's subject in an activation of its own): where J1 is not actief, "expected:
 * &lt;gespeeld&gt; but was: &lt;DelingDoorNul: deling door nul in de regel "zware vondsten": 9 gedeeld door 0 — een
 * numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)&gt;", and for the composite "but
 * was: &lt;DelingDoorNul: deling door nul in de regel "drukte": 9 gedeeld door 0 — …&gt;"; and with two members whose
 * filter faults, a fault per member, "but was: &lt;Evaluatiefouten: aan het fixpunt staan 2 evaluatiefouten, in de
 * regel "zware vondsten" (2×): deling door nul in de regel "zware vondsten": 4 gedeeld door 0 — …; deling door nul in
 * de regel "zware vondsten": 9 gedeeld door 0 — …&gt;", where the evaluation faults once. The rows where J1 is actief
 * and the filter that faults on the way only were green before: they pin what stays.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). With the {@code Filterfout} inserted by
 * {@code insert} instead of {@code logicalInsert}, the record of the transient 0 stayed and the rule faulted at the
 * fixpoint, "[erkenning, zware vondsten] DelingDoorNul: deling door nul in de regel "zware vondsten": 12 gedeeld door 0
 * — …" in both document orders. Without marking a fold whose filter can fault as a read that can fault, the composite's
 * test threw out of the fire where J1 is actief: "but was: &lt;RuntimeException:
 * org.drools.modelcompiler.constraints.LambdaEvalExpression@… : java.lang.RuntimeException:
 * dev.uchar.regelspraak.runtime.Breuk$DelingDoorNul: Breuk deling door 0&gt;".
 */
class TmsFilterfoutWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("zware vondsten", GEHEEL)), List.of("actief", "druk")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("deelnemers", GEHEEL)),
                    List.of("gemeld", "erkend"))),
            List.of(feittypeEenNaarVeel("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
            + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    /** The fold, a new node on every call, so each form reads a fold of its own. */
    static String zwaar() {
        return aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"),
                vergelijking("greater", binair("dividedBy", lezing("gewicht", "Vondst"), lezing("deelnemers", "Vondst")),
                        getal("2")), "null", TELLING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde"
                        + " voldoen: zijn gewicht gedeeld door zijn deelnemers is groter dan 2");
    }

    static final List<String> ZWARE = List.of(rekenregel("zware vondsten", "BEREKENING", "Jutter", "zware vondsten",
            zwaar(), check("actief", false)));

    static final List<String> DRUKTE = List.of(kenmerkregel("drukte", "Jutter", "druk",
            samengesteld("all", 0, List.of(check("actief", false), vergelijking("greater", zwaar(), getal("0"))))));

    static Path document(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels);
    }

    /** J1, actief or not, with V1 and V2 as above, and {@code verwachtingen}. */
    static JsonNode zaak(boolean actief, List<String> verwachtingen) throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), actief ? List.of("actief") : List.of(),
                        Map.of("vondsten", List.of("V1", "V2"))),
                instantieMetRollen("V1", "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of(), Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of(), Map.of())),
                verwachtingen);
    }

    private static String fout(String regel) {
        return "DelingDoorNul: deling door nul in de regel \"" + regel + "\": 9 gedeeld door 0 — een numerieke nul als"
                + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    // ---------------- a fold under a condition that does not hold ----------------

    @Test void een_vouw_onder_een_voorwaarde_die_niet_geldt_faalt_niet() throws Exception {
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(document(ZWARE),
                zaak(false, List.of(waarde("J1", "zware vondsten", null), consistent())),
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals(List.of(), fixpuntfouten(ZWARE, false, null));
    }

    // ---------------- the same fold where the condition holds ----------------

    @Test void een_vouw_onder_een_voorwaarde_die_geldt_geeft_dezelfde_fout() throws Exception {
        JsonNode zaak = zaak(true, List.of(consistent()));
        assertEquals(fout("zware vondsten"), TmsFoutWitnessTest.speel(document(ZWARE), zaak,
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals(fout("zware vondsten"), TmsFoutWitnessTest.speel(document(ZWARE), zaak,
                DroolsBackend.Branch.EXEC_MODEL));
        assertEquals(List.of(), fixpuntfouten(ZWARE, true, fout("zware vondsten")));
    }

    /** With V3 beside them (gewicht 4, deelnemers 0) the filter faults for two members, and the evaluation of the fold
     *  faults once, as the reference interpreter's does. The rule names the least {@code Filterfout} by its sentence,
     *  4 before 9, in every order of the generated rules. */
    @Test void bij_twee_falende_leden_noemt_de_regel_steeds_dezelfde_deling() throws Exception {
        String zin = "DelingDoorNul: deling door nul in de regel \"zware vondsten\": 4 gedeeld door 0 — een numerieke nul"
                + " als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
        JsonNode zaak = TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of("actief"), Map.of("vondsten", List.of("V1", "V2", "V3"))),
                instantieMetRollen("V1", "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of(), Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of(), Map.of()),
                instantieMetRollen("V3", "Vondst", Map.of("gewicht", "4", "deelnemers", "0"), List.of(), Map.of())),
                List.of(consistent()));
        assertEquals(zin, TmsFoutWitnessTest.speel(document(ZWARE), zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        DroolsBackend.Build b = DroolsBackend.build(document(ZWARE), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<String> afwijkend = new ArrayList<>();
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.tmsTranspiler().model().getRules())) {
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            List<Object> bonen = new ArrayList<>(List.of(j1));
            for (Map<String, String> getallen : List.of(Map.of("gewicht", "10", "deelnemers", "2"),
                    Map.of("gewicht", "9", "deelnemers", "0"), Map.of("gewicht", "4", "deelnemers", "0"))) {
                Object v = TmsDocumenten.boon(b.beanLoader(), "Vondst", getallen, List.of());
                zetRol(v, "aanmelder", j1);
                bonen.add(v);
            }
            String uitkomst = TmsFoutWitnessTest.vuur(b, volgorde, bonen, new DefaultAgendaEventListener() {},
                    new TmsFoutWitnessTest.Foutfeiten());
            if (!zin.equals(uitkomst)) afwijkend.add(volgorde.stream().map(Rule::getName).toList() + ": " + uitkomst);
        }
        assertEquals(List.of(), afwijkend);
    }

    // ---------------- a fold in a member of a composite that is not read ----------------

    @Test void een_vouw_in_een_lid_dat_de_samenstelling_niet_leest_faalt_niet() throws Exception {
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(document(DRUKTE),
                zaak(false, List.of(verwacht("J1", "druk", false), consistent())),
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals(List.of(), fixpuntfouten(DRUKTE, false, null));
        JsonNode actief = zaak(true, List.of(consistent()));
        assertEquals(fout("drukte"), TmsFoutWitnessTest.speel(document(DRUKTE), actief,
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals(List.of(), fixpuntfouten(DRUKTE, true, fout("drukte")));
    }

    /** The outcome of the write-back in every order of the rules the branch generates for {@code regels}, with J1 actief
     *  or not: the orders whose outcome differs from {@code verwacht} (null for none), each with what it gave. */
    private static List<String> fixpuntfouten(List<String> regels, boolean actief, String verwacht) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(document(regels), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<String> afwijkend = new ArrayList<>();
        int volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.tmsTranspiler().model().getRules())) {
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), actief ? List.of("actief") : List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "10", "deelnemers", "2"), List.of());
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"), List.of());
            zetRol(v1, "aanmelder", j1);
            zetRol(v2, "aanmelder", j1);
            String uitkomst = TmsFoutWitnessTest.vuur(b, volgorde, List.of(j1, v1, v2), new DefaultAgendaEventListener() {},
                    new TmsFoutWitnessTest.Foutfeiten());
            if (!java.util.Objects.equals(verwacht, uitkomst))
                afwijkend.add(volgorde.stream().map(Rule::getName).toList() + ": " + uitkomst);
            volgordes++;
        }
        assertTrue(volgordes > 1, "één volgorde van de gegenereerde regels");
        return afwijkend;
    }

    // ---------------- a filter that faults on the way only ----------------

    private static final String DEELNEMERS = attribuutgroep("deelnemers van een vondst", "Vondst", "deelnemers", List.of(
            standaard(check("erkend", false), getal("3")),
            standaard(check("gemeld", false), getal("0"))));

    /** The fold with its division guarded, "… die aan alle volgende voorwaarden voldoen: zijn deelnemers is gevuld; zijn
     *  gewicht gedeeld door zijn deelnemers is groter dan 2": the reference interpreter then divides by no leeg on its
     *  way and keeps no §6.5 deposit there (ch. 6.1, "Recorded residuals"), which it did without the guard ("MISMATCH:
     *  expected consistent, but the channel carries: zware vondsten × J1", measured 2026-10-08). */
    static String bewaaktZwaar() {
        return aggregatie("count", VONDSTEN, "Vondst", bronrol("aanmelding", "vondst"),
                samengesteld("all", 0, List.of(TmsDocumenten.predicaat("filled", lezing("deelnemers", "Vondst")),
                        vergelijking("greater", binair("dividedBy", lezing("gewicht", "Vondst"),
                                lezing("deelnemers", "Vondst")), getal("2")))), "null", TELLING,
                "het aantal vondsten van de Jutter die aan alle volgende voorwaarden voldoen: zijn deelnemers is"
                        + " gevuld; zijn gewicht gedeeld door zijn deelnemers is groter dan 2");
    }

    static final List<String> TIJDELIJK = List.of(kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false)),
            rekenregel("zware vondsten", "BEREKENING", "Jutter", "zware vondsten", bewaaktZwaar(), "null"));

    static Path tijdelijk(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(DEELNEMERS));
    }

    /** J1 reports V1 (gewicht 12) and V2 (gewicht 9), both gemeld: deelnemers 3 for both, 12 and 9 divided by 3 is more
     *  than 2, so zware vondsten 2. Exact quotients: the ASP runner refuses a division that does not terminate without
     *  an explicit rounding. */
    static JsonNode tijdelijkeZaak() throws Exception {
        return TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten", List.of("V1", "V2"))),
                instantieMetRollen("V1", "Vondst", Map.of("gewicht", "12"), List.of("gemeld"), Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("gewicht", "9"), List.of("gemeld"), Map.of())),
                List.of(waarde("J1", "zware vondsten", "2"), waarde("V1", "deelnemers", "3"),
                        waarde("V2", "deelnemers", "3"), consistent()));
    }

    /** Both orders of the document, and a seeded sample of the orders of the generated rules: no fault at the fixpoint,
     *  zware vondsten 2, and at least one order in which the filter divided by the lower default's 0 on the way. */
    @Test void een_filter_dat_onderweg_door_nul_deelt_faalt_niet_aan_het_fixpunt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TIJDELIJK)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(tijdelijk(volgorde), tijdelijkeZaak(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        DroolsBackend.Build b = DroolsBackend.build(tijdelijk(TIJDELIJK), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int onderweg = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "12"), List.of("gemeld"));
            Object v2 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9"), List.of("gemeld"));
            zetRol(v1, "aanmelder", j1);
            zetRol(v2, "aanmelder", j1);
            Foutsporen sporen = new Foutsporen();
            try {
                TmsDocumenten.vuur(b, volgorde, List.of(j1, v1, v2), sessie -> sessie.addEventListener(sporen),
                        sessie -> { });
            } catch (RuntimeException e) {
                throw new AssertionError("volgorde " + namen + ": " + e, e);
            }
            Object zwaar = TmsDocumenten.lees(j1, "zware vondsten");
            assertTrue(zwaar instanceof BigDecimal d && d.compareTo(BigDecimal.valueOf(2)) == 0,
                    () -> "zware vondsten in volgorde " + namen + ": " + zwaar);
            if (sporen.ingevoegd > 0) onderweg++;
        }
        assertTrue(onderweg > 0, "geen volgorde liet het filter delen door de 0 van de lagere default");
    }

    // ---------------- helpers ----------------

    /** Counts the facts a session inserts that record a fault in a fold's filter, by the simple name of their class:
     *  the fault of the reading rule, and the record of a member whose filter faults. Named as text, so this class also
     *  compiles against the branch before such a record existed. */
    static final class Foutsporen implements RuleRuntimeEventListener {
        int ingevoegd;

        @Override public void objectInserted(ObjectInsertedEvent e) {
            String naam = e.getObject().getClass().getSimpleName();
            if (naam.equals("Fout") || naam.equals("Filterfout")) ingevoegd++;
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {}
    }

    /** Sets the to-one reference field {@code rol} of {@code boon} to {@code doel}, as the case reader does. */
    private static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(dev.uchar.regelspraak.transpiler.kern.Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }
}
