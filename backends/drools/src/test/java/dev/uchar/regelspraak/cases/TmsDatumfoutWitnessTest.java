package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUMTIJD;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.datumwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.eenheid;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waardeJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The faults of date arithmetic on the exec-model branch (RUNTIME-SEMANTICS ch. 3.8): a duration count outside
 * [−2⁶², 2⁶² − 1] and a datum-tijd step that leaves the representable millisecond range are evaluation faults, which on
 * this branch are facts that count only where they stand at the fixpoint (PLAN §9, 2026-10-09), named as the reference
 * interpreter names them. Until this card they left the fire as an exception, so a count that lies outside the range
 * only on an intermediate state ended the session where the fixpoint has a value.
 *
 * <p>The transient document: a Vondst with a vinddatum, "Een Vondst is erkend indien hij gemeld is", a defaults group
 * on the wachtdagen, higher "op 3 indien hij erkend is" and lower "op 18446744073709551617 indien hij gemeld is", and
 * "De meldtermijn van een Vondst moet berekend worden als zijn vinddatum plus zijn wachtdagen". V1 (gemeld, vinddatum
 * 2026-03-01): wachtdagen 3, meldtermijn 2026-03-04, consistent. Where the lower default decides before the erkenning,
 * the meldtermijn steps 2⁶⁴ + 1 days, out of range; that fault goes again when the higher default decides.
 *
 * <p>The default document: the same group on the meldtermijn itself, higher "op zijn vinddatum plus 3 dagen indien hij
 * erkend is" and lower "op zijn vinddatum plus 18446744073709551617 dagen indien hij gemeld is": the fault arises in the
 * consequence of the lower default and goes when the higher one displaces it; and with the step in the justification
 * of the lower default, "op zijn vinddatum indien zijn vinddatum plus 18446744073709551617 dagen later is dan
 * 2026-01-01", the fault arises in its test and goes the same way.
 *
 * <p>The standing documents: the count and the step stand at the fixpoint, and the session reports them with the
 * sentence the reference interpreter gives, "duration count 18446744073709551617 is out of range" and "datum-tijd
 * arithmetic leaves the representable millisecond range".
 *
 * <p><b>Measured</b> (2026-10-09, on every order of every document): the reference interpreter's runner passes the
 * transient and the default documents, where it decides the defaults group after the erkenning, and faults on the
 * standing ones in the two sentences above. The ASP runner declines all of them at translation, its integers being 32
 * bits ("value 18446744073709551617 at 0 decimals exceeds clingo's integer range"). This branch, at the claim of the
 * card, threw both faults out of the fire, in every order of the transient document as well (below).
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim): "[erkenning, meldtermijn]
 * ConsequenceException: Exception executing consequence for rule "meldtermijn" in dev.uchar.regelspraak.tms:
 * java.lang.IllegalStateException: duration count 18446744073709551617 is out of range", the same in the other order and
 * out of "defaults A:Vondst.meldtermijn: default 1" for the first default document, and for the standing ones that
 * exception instead of the fault fact, "ConsequenceException: … rule "sluittijdstip" …
 * java.lang.IllegalStateException: datum-tijd arithmetic leaves the representable millisecond range".
 */
class TmsDatumfoutWitnessTest {

    private static final String GEHEEL = getaltype(true, 0, null), DAGEN = getaltype(true, 0, eenheid("dagen"));

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst", List.of(
            attribuut("vinddatum", DATUM), attribuut("wachtdagen", DAGEN), attribuut("meldtermijn", DATUM)),
            List.of("gemeld", "erkend"))), List.of());

    private static final String TE_GROOT = "18446744073709551617";

    static final String ERKENNING = kenmerkregel("erkenning", "Vondst", "erkend", check("gemeld", false));

    static final String MELDTERMIJN = rekenregel("meldtermijn", "BEREKENING", "Vondst", "meldtermijn",
            binair("plus", lezing("vinddatum", "Vondst"), lezing("wachtdagen", "Vondst"), DATUM), "null");

    static final String WACHTDAGEN = attribuutgroep("wachtdagen van een vondst", "Vondst", "wachtdagen", List.of(
            standaard(check("erkend", false), literaal("3", GEHEEL)),
            standaard(check("gemeld", false), literaal(TE_GROOT, GEHEEL))));

    static JsonNode zaak() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(instantieMet("V1", "Vondst",
                Map.of("vinddatum", datumwaarde("2026-03-01")), List.of("gemeld"))),
                List.of(waardeJson("V1", "meldtermijn", datumwaarde("2026-03-04")), consistent()));
    }

    @Test void een_aantal_dat_alleen_op_een_tussenstand_buiten_het_bereik_ligt_telt_niet() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(ERKENNING, MELDTERMIJN))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(TmsDocumenten.document(MODEL, volgorde, List.of(),
                    List.of(WACHTDAGEN)), zaak(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
        assertTussenstand(TmsDocumenten.document(MODEL, List.of(ERKENNING, MELDTERMIJN), List.of(), List.of(WACHTDAGEN)));
    }

    /** The date steps of the lower default, in its consequence and in its justification. */
    static List<String> defaultgroepen() {
        String vinddatum = lezing("vinddatum", "Vondst");
        String drie = literaal("3", DAGEN), groot = literaal(TE_GROOT, DAGEN);
        return List.of(
                attribuutgroep("meldtermijn van een vondst", "Vondst", "meldtermijn", List.of(
                        standaard(check("erkend", false), binair("plus", vinddatum, drie, DATUM)),
                        standaard(check("gemeld", false), binair("plus", vinddatum, groot, DATUM)))),
                attribuutgroep("meldtermijn van een vondst", "Vondst", "meldtermijn", List.of(
                        standaard(check("erkend", false), binair("plus", vinddatum, drie, DATUM)),
                        standaard(vergelijking("later", binair("plus", vinddatum, groot, DATUM),
                                literaal("2026-01-01", DATUM)), vinddatum))));
    }

    @Test void een_datumstap_buiten_het_bereik_in_een_verdrongen_default_telt_niet() throws Exception {
        for (String groep : defaultgroepen()) {
            Path ir = TmsDocumenten.document(MODEL, List.of(ERKENNING), List.of(), List.of(groep));
            assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaak(), DroolsBackend.Branch.EXEC_MODEL));
            assertTussenstand(ir);
        }
    }

    /** Every order of the generated rules, on V1: the fire reaches its fixpoint without throwing, no fault stands
     *  there, the meldtermijn is 2026-03-04, and in at least one order the out-of-range step happened and its fault
     *  went again. */
    private static void assertTussenstand(Path ir) throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        int tussenstand = 0, volgordes = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.execModelTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of(), List.of("gemeld"));
            v1.getClass().getMethod(dev.uchar.regelspraak.transpiler.kern.Namen.setter("vinddatum"),
                    java.time.LocalDate.class).invoke(v1, java.time.LocalDate.of(2026, 3, 1));
            TmsLetWitnessTest.Feiten feiten = new TmsLetWitnessTest.Feiten();
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(v1), sessie -> sessie.addEventListener(feiten)),
                    () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertEquals(java.time.LocalDate.of(2026, 3, 4), TmsDocumenten.lees(v1, "meldtermijn"),
                    () -> "meldtermijn in volgorde " + namen);
            if (feiten.fouten > 0) tussenstand++;
            volgordes++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet de lagere default eerst beslissen");
        assertTrue(volgordes > 2);
    }

    private static final String TIJDMODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Getijpost", List.of(
            attribuut("meldtijdstip", DATUMTIJD), attribuut("wachttijd", getaltype(true, 0, eenheid("uur"))),
            attribuut("sluittijdstip", DATUMTIJD), attribuut("vinddatum", DATUM), attribuut("wachtdagen", DAGEN),
            attribuut("meldtermijn", DATUM)), List.of())), List.of());

    @Test void een_staande_datumfout_geeft_de_zin_van_de_referentie() throws Exception {
        String sluiten = rekenregel("sluittijdstip", "BEREKENING", "Getijpost", "sluittijdstip",
                binair("plus", lezing("meldtijdstip", "Getijpost"), lezing("wachttijd", "Getijpost"), DATUMTIJD), "null");
        String melden = rekenregel("meldtermijn", "BEREKENING", "Getijpost", "meldtermijn",
                binair("plus", lezing("vinddatum", "Getijpost"), lezing("wachtdagen", "Getijpost"), DATUM), "null");
        JsonNode uren = TmsDocumenten.zaakOp(null, List.of(instantieMet("G1", "Getijpost", Map.of(
                "meldtijdstip", "{\"dateTime\":\"2024-01-01T00:00:00\"}", "wachttijd", getalwaarde("2000000000000")),
                List.of())), List.of(consistent()));
        JsonNode dagen = TmsDocumenten.zaakOp(null, List.of(instantieMet("G1", "Getijpost", Map.of(
                "vinddatum", datumwaarde("2026-03-01"), "wachtdagen", getalwaarde(TE_GROOT)), List.of())),
                List.of(consistent()));
        assertEquals(List.of("Rekenfout: fout in de regel \"sluittijdstip\": datum-tijd arithmetic leaves the"
                        + " representable millisecond range",
                        "Rekenfout: fout in de regel \"meldtermijn\": duration count " + TE_GROOT + " is out of range"),
                List.of(TmsFoutWitnessTest.speel(TmsDocumenten.document(TIJDMODEL, List.of(sluiten)), uren,
                                DroolsBackend.Branch.EXEC_MODEL),
                        TmsFoutWitnessTest.speel(TmsDocumenten.document(TIJDMODEL, List.of(melden)), dagen,
                                DroolsBackend.Branch.EXEC_MODEL)));
    }
}
