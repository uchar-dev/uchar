package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The member values of an object creation are the input of the object it makes, as the reference interpreter reads
 * them (RUNTIME-SEMANTICS ch. 5.5, 1.2g, 7.8), and a division in an initialisation beside a writer counts that writer
 * per concrete type (WF-079, ch. 7.10). Fix round 1 of step 3 of card tms-expressievormen-en-rekendatum.
 *
 * <p>The creation document: "Een Jutter heeft een melding (een Jutmelding, a subtype of Melding) met de waarde zijn
 * vangst", and "De terug van een Jutter moet berekend worden als de som van de waarde van al zijn meldingen, of 0",
 * through which the case reads the made object. J1 has vangst 7. Without a writer of the member the value of the
 * making stands: terug 7. A standing rule on the supertype, "De waarde van een Melding moet gesteld worden op 99",
 * displaces it as it displaces case input: 99. A rule on the created type itself is outside the standing domain (the
 * creation is another writer, ch. 7.8), and where it computes leeg it erases the value: 0. An initialisation on the
 * member finds it filled: 7; where the creation's own value is leeg (it reads the bonus, which no case sets), the
 * initialisation fills it: 5. Two rules on the member that write 98 and 97 are the fault of ch. 5.7, the writers in the
 * order of the document, the object named as made by a creation. Every order of the rules in the document.
 *
 * <p>The sister document: "Het doel van een Drijfgoed moet geïnitialiseerd worden op zijn a gedeeld door 2" beside "Het
 * doel van een Wrakgoed moet gesteld worden op 7", with the a derived from c ("De a van een Vondst = zijn c plus 2"). The
 * two writers never write one instance, so the division is no fault on an intermediate state: D1 (c 2) 2, W1 7,
 * consistent, in every order of the three rules. A writer on the supertype beside it writes the Drijfgoed too, and the
 * refusal that waits on card fout-op-tussenstand names it.
 *
 * <p><b>Measured</b> (2026-10-09, these documents in every order, written out with their cases and run through
 * {@code ref/bin/runner.exe} and {@code backends/clingo/bin/clingo_runner.exe}): ref gives terug 99, 7, 0, 7 and 5,
 * faults on the two rules, "two writers of one target disagree: rule "waarde volgens de vangst" writes 98 and rule
 * "waarde volgens het tarief" writes 97 to the attribute waarde of oc0::J1, a Jutmelding", the writers in the order of
 * the document, and passes the sister document in all six orders. clingo agrees on 7 without a writer, on 5, on the
 * fault and on the sister document, and differs from ref on the made object where a writer meets the creation's value:
 * 106 with the standing rule, 12 with the initialisation and 7 with the rule that computes leeg; the main session takes
 * the first to a card of its own.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-09 on {@code 1738bf61}, the tests unchanged): four of the seven
 * tests. The standing rule: "Schrijversconflict geen fixpunt: twee schrijvers van één doel geven verschillende waarden
 * — de regel "waarde van een melding" schrijft 99 en de regel "meld de vangst" schrijft 7 in het attribuut waarde van
 * een aangemaakte Jutmelding", the creation's stated {@code Waarde} meeting the rule's. The rule that computes leeg and
 * the two rules: refused, "tms-transpiler SKIP: regel: waarde van een jutmelding — een rekenende regel buiten het
 * staande domein op een lid dat een objectcreatie bij het maken zet ('waarde' van Melding, gezet door "meld de
 * vangst"): …". The sister document: "tms-transpiler SKIP: regel: doel van drijfgoed — een deling in een
 * initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'doel' van Vondst wordt ook geschreven door
 * "doel van wrakgoed"): …". The creation without a writer, the initialisation beside it and the creation's leeg were
 * green already: they pin what the fix keeps.
 */
class TmsCreatieInvoerWitnessTest {

    // ---------------- the member values of a creation are the input of the made object ----------------

    private static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("vangst", GEHEEL), attribuut("bonus", GEHEEL),
                    attribuut("terug", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Melding", List.of(attribuut("waarde", GEHEEL), attribuut("toeslag", GEHEEL)),
                    List.of()),
            TmsDocumenten.subtype("Jutmelding", "Melding", List.of(), List.of())),
            List.of(TmsDocumenten.feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Jutmelding",
                    "meldingen")));

    /** "De terug van een Jutter moet berekend worden als de som van de waarde van al zijn meldingen, of 0": the case
     *  reads the member of the made object through it. */
    private static final String TERUG = rekenregel("terug van een jutter", "BEREKENING", "Jutter", "terug",
            TmsDocumenten.aggregatie("sum", lezing("waarde", "Jutmelding"), "Jutmelding",
                    TmsDocumenten.bronrol("jutmelding", "melding"), "null", getal("0"), GEHEEL,
                    "de som van de waarde van al zijn meldingen of 0"), "null");

    /** On the supertype, so the creation does not take it out of the standing domain (ch. 7.8, "other writer"). */
    private static final String STAAND = rekenregel("waarde van een melding", "GELIJKSTELLING", "Melding", "waarde",
            getal("99"), "null");

    /** On the created type itself, so outside the standing domain; the toeslag is never set, so it computes leeg. */
    private static final String LEEG = rekenregel("waarde van een jutmelding", "GELIJKSTELLING", "Jutmelding", "waarde",
            lezing("toeslag", "Jutmelding"), "null");

    private static final String INITIALISATIE = rekenregel("standaardwaarde", "INITIALISATIE", "Jutmelding", "waarde",
            getal("5"), "null");

    private static final List<String> TWEE = List.of(
            rekenregel("waarde volgens de vangst", "GELIJKSTELLING", "Jutmelding", "waarde",
                    binair("plus", lezing("toeslag", "Jutmelding"), getal("98")), "null"),
            rekenregel("waarde volgens het tarief", "GELIJKSTELLING", "Jutmelding", "waarde", getal("97"), "null"));

    /** The creation, its waarde the vangst of its subject, or the bonus, which no case sets. */
    private static String creatie(String bron) {
        return TmsCreatieWitnessTest.objectcreatie("meld de vangst", "Jutter", "Jutmelding",
                List.of(TmsCreatieWitnessTest.init("waarde", lezing(bron, "Jutter"))), "null", "jutmelding", "melding");
    }

    private static Path document(List<String> regels, String bron) throws Exception {
        return TmsCreatieWitnessTest.document(MODEL, regels, List.of(), List.of(creatie(bron)), List.of(), List.of());
    }

    private static JsonNode zaak(String terug) throws Exception {
        return TmsDocumenten.zaak(List.of(instantie("J1", "Jutter", Map.of("vangst", "7"), List.of(), Map.of())),
                List.of(waarde("J1", "terug", terug)));
    }

    /** Every order of {@code regels} in the document plays {@code zaak(terug)}. */
    private static void speelt(List<String> regels, String bron, String terug) throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertEquals("gespeeld", TmsFoutWitnessTest.speelBeide(document(volgorde, bron), zaak(terug)),
                    () -> "volgorde " + namen);
            volgordes++;
        }
        assertTrue(volgordes > 0);
    }

    @Test void een_staande_regel_verdringt_de_waarde_van_de_creatie() throws Exception {
        speelt(List.of(TERUG, STAAND), "vangst", "99");
    }

    @Test void zonder_schrijver_blijft_de_waarde_van_de_creatie_staan() throws Exception {
        speelt(List.of(TERUG), "vangst", "7");
    }

    @Test void een_regel_buiten_het_staande_domein_die_leeg_berekent_wist_de_waarde_van_de_creatie() throws Exception {
        speelt(List.of(TERUG, LEEG), "vangst", "0");
    }

    @Test void een_initialisatie_vindt_het_lid_van_de_creatie_gevuld() throws Exception {
        speelt(List.of(TERUG, INITIALISATIE), "vangst", "7");
    }

    @Test void een_lege_waarde_van_de_creatie_laat_de_initialisatie_vullen() throws Exception {
        speelt(List.of(TERUG, INITIALISATIE), "bonus", "5");
    }

    @Test void twee_schrijvers_op_het_lid_van_de_creatie_met_verschillende_waarden_zijn_de_fout() throws Exception {
        List<String> regels = new ArrayList<>(TWEE);
        regels.add(TERUG);
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = document(volgorde, "vangst");
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> CaseRunner.speelCase(ir, zaak("0"), DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + namen);
            boolean vangstEerst = namen.indexOf("waarde volgens de vangst") < namen.indexOf("waarde volgens het tarief");
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \""
                    + (vangstEerst ? "waarde volgens de vangst\" schrijft 98 en de regel \"waarde volgens het tarief\" schrijft 97"
                                   : "waarde volgens het tarief\" schrijft 97 en de regel \"waarde volgens de vangst\" schrijft 98")
                    + " in het attribuut waarde van een aangemaakte Jutmelding; divergentie is een fout (RUNTIME-SEMANTICS"
                    + " ch. 5.7)", e.getMessage(), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    // ---------------- a division in an initialisation, and a writer on a sister subtype ----------------

    private static final String ZUSTERMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("a", GEHEEL), attribuut("c", GEHEEL),
                    attribuut("doel", GEHEEL)), List.of()),
            TmsDocumenten.subtype("Drijfgoed", "Vondst", List.of(), List.of()),
            TmsDocumenten.subtype("Wrakgoed", "Vondst", List.of(), List.of())), List.of());

    private static final List<String> ZUSTERS = List.of(
            rekenregel("doel van drijfgoed", "INITIALISATIE", "Drijfgoed", "doel",
                    binair("dividedBy", lezing("a", "Drijfgoed"), getal("2")), "null"),
            rekenregel("doel van wrakgoed", "GELIJKSTELLING", "Wrakgoed", "doel", getal("7"), "null"),
            rekenregel("a van een vondst", "BEREKENING", "Vondst", "a", binair("plus", lezing("c", "Vondst"), getal("2")),
                    "null"));

    @Test void een_deling_in_een_initialisatie_naast_een_schrijver_op_een_zustersubtype() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("D1", "Drijfgoed", Map.of("c", "2"), List.of(), Map.of()),
                        instantie("W1", "Wrakgoed", Map.of(), List.of(), Map.of())),
                List.of(waarde("D1", "doel", "2"), waarde("W1", "doel", "7"), TmsDocumenten.consistent()));
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(ZUSTERS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertEquals("gespeeld", TmsFoutWitnessTest.speelBeide(TmsDocumenten.document(ZUSTERMODEL, volgorde), zaak), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
        List<String> metSupertype = new ArrayList<>(ZUSTERS);
        metSupertype.add(rekenregel("doel van een vondst", "GELIJKSTELLING", "Vondst", "doel", getal("9"),
                TmsDocumenten.vergelijking("greater", lezing("a", "Vondst"), getal("100"))));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocumenten.document(ZUSTERMODEL, metSupertype), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().startsWith("transpiler SKIP: regel: doel van drijfgoed — een deling in een"
                + " initialisatie waarvan het doel nog een schrijver heeft ('dividedBy'; 'doel' van Vondst wordt ook"
                + " geschreven door \"doel van een vondst\"): "), e.getMessage());
    }
}
