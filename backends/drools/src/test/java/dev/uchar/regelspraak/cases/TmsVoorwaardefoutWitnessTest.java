package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultAgendaEventListener;

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
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistentie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An evaluation fault in a condition on the truth-maintenance branch. A condition is tested in the left-hand side,
 * where Drools cannot take a fault as a fact: a division by a numeric zero there left the fire as an exception of the
 * engine that named neither the rule nor the division. Its test now answers that the condition does not hold where it
 * faults, and a rule of the form's own inserts the fault logically, so the fault stands while the reads it rested on
 * stand and is reported at the fixpoint, as a fault in a value is ({@link TmsFoutWitnessTest}).
 *
 * <p>The transient document: a Vondst with {@code taxatie}, {@code opbrengst} and {@code bergloon}, "Een Vondst is
 * erkend indien hij gemeld is", the defaults group of {@link TmsFoutWitnessTest} on the bergloon (higher "op zijn
 * taxatie indien hij erkend is", lower "op 0 indien hij gemeld is"), and "Een Vondst is zwaar indien zijn opbrengst
 * gedeeld door zijn bergloon groter is dan 2". V1 (gemeld, taxatie 30, opbrengst 90): bergloon 30, zwaar, consistent;
 * where the lower default decides first, the condition divides 90 by the transient 0. The reference interpreter's
 * runner and the ASP runner passed the case in both orders of the document's rules (measured 2026-10-08).
 *
 * <p>The standing documents: one form whose condition or assertion reads "zijn opbrengst gedeeld door zijn deelnemers"
 * beside the erkenning, per kind of form that carries a condition here: a characteristic attribution, an assignment,
 * an initialisation and a consistency. V1 (gemeld, opbrengst 100, deelnemers 0 in the input) faults at the fixpoint in
 * every order, with the sentence the exec-model branch names the same fault with out of its fire, where it lowers the
 * form. Both runners faulted on each of them, "division by numeric zero" (measured 2026-10-08).
 *
 * <p><b>Red before the change</b> (measured 2026-10-08 on the branch at its claim): every row of this branch left the
 * fire with the engine's exception, "[erkenning, zwaarte] RuntimeException:
 * org.drools.modelcompiler.constraints.LambdaEvalExpression@e410d4c : java.lang.RuntimeException:
 * dev.uchar.regelspraak.runtime.Breuk$DelingDoorNul: Breuk deling door 0", the transient document in both orders and
 * each standing form, and the rows that fire a session themselves stopped in the fire with it, "de fire wierp in
 * volgorde [erkenning, zwaarte]: java.lang.RuntimeException: …LambdaEvalExpression@e410d4a : …". The exec-model rows
 * were green before.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed): with the condition's fault inserted by
 * {@code insert} instead of {@code logicalInsert}, the transient fault stayed, "[erkenning, zwaarte] DelingDoorNul:
 * deling door nul in de regel "zwaarte": 90 gedeeld door 0 — …" in both document orders and in the sample.
 */
class TmsVoorwaardefoutWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("taxatie", GEHEEL), attribuut("opbrengst", GEHEEL), attribuut("deelnemers", GEHEEL),
                    attribuut("bergloon", GEHEEL), attribuut("toeslag", GEHEEL)),
            List.of("gemeld", "erkend", "zwaar"))), List.of());

    private static final String ERKENNING = kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false));

    // ---------------- a transient fault in a condition ----------------

    static final List<String> TIJDELIJK = List.of(ERKENNING,
            kenmerkregel("zwaarte", "Vondst", "zwaar", vergelijking("greater",
                    binair("dividedBy", lezing("opbrengst", "Vondst"), lezing("bergloon", "Vondst")), getal("2"))));

    static final String GROEP = attribuutgroep("bergloon van een vondst", "Vondst", "bergloon", List.of(
            standaard(check("erkend", false), lezing("taxatie", "Vondst")),
            standaard(check("gemeld", false), getal("0"))));

    static JsonNode tijdelijkeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("taxatie", "30", "opbrengst", "90"), List.of("gemeld"), Map.of())),
                List.of(waarde("V1", "bergloon", "30"), verwacht("V1", "zwaar", true), consistent()));
    }

    static Path tijdelijk(List<String> regels) throws Exception {
        return TmsDocumenten.document(MODEL, regels, List.of(), List.of(GROEP));
    }

    @Test void een_tijdelijke_fout_in_een_voorwaarde_verdwijnt_aan_het_fixpunt_in_elke_volgorde_van_het_document()
            throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(TIJDELIJK)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(tijdelijk(volgorde), tijdelijkeZaak(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(2, uitkomsten.size());
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(tijdelijk(TIJDELIJK), tijdelijkeZaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The generated rules in a seeded sample of orders, at least one of which must test the condition against the
     *  transient 0 of the lower default: there a fault fact is inserted, and it is gone again at the fixpoint. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(tijdelijk(TIJDELIJK), DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        int opDeTussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261008L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("taxatie", "30", "opbrengst", "90"),
                    List.of("gemeld"));
            TmsFoutWitnessTest.Foutfeiten feiten = new TmsFoutWitnessTest.Foutfeiten();
            String fout = TmsFoutWitnessTest.vuur(b, volgorde, List.of(v1), new DefaultAgendaEventListener() {}, feiten);
            assertNull(fout, () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            TmsFoutWitnessTest.assertGetal("30", TmsDocumenten.lees(v1, "bergloon"), "bergloon in volgorde " + namen);
            assertTrue(TmsDocumenten.heeft(v1, "zwaar"), () -> "zwaar in volgorde " + namen);
            if (feiten.ingevoegd > 0) opDeTussenstand++;
        }
        assertTrue(opDeTussenstand > 0, "geen volgorde toetste de voorwaarde tegen de tijdelijke 0 van de lagere default");
    }

    // ---------------- a fault in a condition that stands ----------------

    private static final String DEELNEMERS = binair("dividedBy", lezing("opbrengst", "Vondst"),
            lezing("deelnemers", "Vondst"));

    /** One document per kind of form with a condition: its name, its rules and its consistencies, and whether the
     *  exec-model branch lowers it. That branch refuses a consistency whose assertion divides ("consistentie (schending
     *  buiten Phase-D scope)", measured 2026-10-08), so that row compares this branch alone. */
    record Vorm(String naam, List<String> regels, List<String> consistenties, boolean execModel) {}

    static final List<Vorm> VORMEN = List.of(
            new Vorm("zwaarte", List.of(ERKENNING, kenmerkregel("zwaarte", "Vondst", "zwaar",
                    vergelijking("greater", DEELNEMERS, getal("10")))), List.of(), true),
            new Vorm("toeslag", List.of(ERKENNING, rekenregel("toeslag", "GELIJKSTELLING", "Vondst", "toeslag",
                    getal("5"), vergelijking("greater", DEELNEMERS, getal("10")))), List.of(), true),
            new Vorm("standaardtoeslag", List.of(ERKENNING, rekenregel("standaardtoeslag", "INITIALISATIE", "Vondst",
                    "toeslag", getal("5"), vergelijking("greater", DEELNEMERS, getal("10")))), List.of(), true),
            new Vorm("controle", List.of(ERKENNING),
                    List.of(consistentie("controle", "Vondst", vergelijking("greater", DEELNEMERS, getal("0")))), false));

    static JsonNode blijvendeZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"), List.of("gemeld"),
                        Map.of())),
                List.of(consistent()));
    }

    private static String melding(String regel) {
        return "DelingDoorNul: deling door nul in de regel \"" + regel + "\": 100 gedeeld door 0 — een numerieke nul als"
                + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";
    }

    @Test void een_blijvende_fout_in_een_voorwaarde_geeft_in_elke_volgorde_de_melding_van_de_exec_modeltak()
            throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Vorm v : VORMEN) {
            for (List<String> volgorde : TmsDocumenten.permutaties(v.regels())) {
                List<String> namen = TmsDocumenten.regelnamen(volgorde);
                uitkomsten.add(v.naam() + " " + namen + " " + TmsFoutWitnessTest.speel(
                        TmsDocumenten.document(MODEL, volgorde, v.consistenties()), blijvendeZaak(),
                        DroolsBackend.Branch.TRUTH_MAINTENANCE));
                verwacht.add(v.naam() + " " + namen + " " + melding(v.naam()));
            }
            if (!v.execModel()) continue;
            uitkomsten.add(v.naam() + " exec-model " + TmsFoutWitnessTest.speel(
                    TmsDocumenten.document(MODEL, v.regels(), v.consistenties()), blijvendeZaak(),
                    DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(v.naam() + " exec-model " + melding(v.naam()));
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(10, uitkomsten.size());
    }

    /** The fire reaches its fixpoint without throwing in every order of the generated rules of each document (the form,
     *  its fault rule, its deposit rule and the erkenning: 24 orders each; six until the deposit rule of a condition,
     *  fixround 1 of card tms-expressievormen-en-rekendatum), and the write-back reports the fault. */
    @Test void de_fout_in_een_voorwaarde_telt_aan_het_fixpunt_en_niet_in_de_fire() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Vorm v : VORMEN) {
            DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, v.regels(), v.consistenties()),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE);
            for (List<Rule> volgorde : TmsDocumenten.permutaties(b.tmsTranspiler().model().getRules())) {
                List<String> namen = volgorde.stream().map(Rule::getName).toList();
                Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("opbrengst", "100", "deelnemers", "0"),
                        List.of("gemeld"));
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.vuur(b, volgorde, List.of(v1),
                        new DefaultAgendaEventListener() {}, new TmsFoutWitnessTest.Foutfeiten()));
                verwacht.add(namen + " " + melding(v.naam()));
            }
        }
        assertEquals(verwacht, uitkomsten);
        assertEquals(96, uitkomsten.size());
    }
}
