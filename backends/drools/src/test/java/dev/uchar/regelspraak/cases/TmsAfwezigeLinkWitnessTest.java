package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.padcheck;
import static dev.uchar.regelspraak.cases.TmsDocumenten.padlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.predicaat;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reads over a role on the truth-maintenance branch where the link is absent (RUNTIME-SEMANTICS ch. 5.8). A Vondst
 * reads over its vinder, a Jutter: the derived tarief ("Het tarief van een Jutter moet berekend worden als zijn
 * basistarief plus 1, indien hij erkend is"), the derived characteristic erkend ("Een Jutter is erkend indien hij
 * aangemeld is") and the input basistarief. Six characteristics of the Vondst test those reads: the tarief gevuld,
 * leeg, and unequal to 5, the vinder erkend and niet erkend, the basistarief gevuld; two attributes compute over them:
 * the tarief maal 2 and the basistarief maal 2. V1's vinder J1 is aangemeld (basistarief 4, so tarief 5), V2's vinder
 * J2 is not (basistarief 4, tarief leeg), and V3 has no vinder. Rule 1 of ch. 5.8 makes V3's reads leeg, so V3 answers
 * the leeg and gevuld tests and the comparison as V2 does, and both compute leeg maal 2 as 0 (RUNTIME-SEMANTICS ch.
 * 1.2); rule 3 makes the check of erkend over the absent link onwaar in both polarities, so V3 is neither begeleid nor
 * verdacht, where V2 is verdacht. A derived member over the role is read through its path standing, which an absent
 * link gives the value null; the input basistarief is read off the Vondst's bean through the reference field.
 *
 * <p>Ten rules have too many orders to try each: the document order, its reverse and a seeded sample.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-08 on the branch's first step): the sample row was refused,
 * "tms-transpiler SKIP: regel: tarief — een attribuutdoel (berekening van 'tarief' van Jutter): de
 * truth-maintenance-tak leidt nog alleen kenmerken af". The row on the exec-model branch was green from the start.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). Without the path-standing rule for an absent link,
 * V3's reads over its role had no match, and the document order answered "case: kenmerk onbeheerd: verwacht aanwezig,
 * werkelijk afwezig; kenmerk afwijkend: verwacht aanwezig, werkelijk afwezig; attribuut beloning: verwacht 0, werkelijk
 * leeg". With an absent link read as onwaar by the characteristic check, it answered "case: kenmerk verdacht: verwacht
 * afwezig, werkelijk aanwezig".
 */
class TmsAfwezigeLinkWitnessTest {

    private static final String TARIEF = padlezing("tarief", "Jutter", "vinding", "vinder");
    private static final String BASISTARIEF = padlezing("basistarief", "Jutter", "vinding", "vinder");

    private static final List<String> REGELS = List.of(
            kenmerkregel("erkenning", "Jutter", "erkend", check("aangemeld", false)),
            rekenregel("tarief", "BEREKENING", "Jutter", "tarief",
                    binair("plus", lezing("basistarief", "Jutter"), getal("1")), check("erkend", false)),
            kenmerkregel("toewijzing", "Vondst", "toegewezen", predicaat("filled", TARIEF)),
            kenmerkregel("onbeheerd", "Vondst", "onbeheerd", predicaat("empty", TARIEF)),
            kenmerkregel("afwijking", "Vondst", "afwijkend", vergelijking("notEqual", TARIEF, getal("5"))),
            kenmerkregel("begeleiding", "Vondst", "begeleid", padcheck("erkend", false, "vinding", "vinder")),
            kenmerkregel("verdenking", "Vondst", "verdacht", padcheck("erkend", true, "vinding", "vinder")),
            kenmerkregel("prijzing", "Vondst", "geprijsd", predicaat("filled", BASISTARIEF)),
            rekenregel("beloning", "BEREKENING", "Vondst", "beloning", binair("times", TARIEF, getal("2")), "null"),
            rekenregel("vergoeding", "BEREKENING", "Vondst", "vergoeding", binair("times", BASISTARIEF, getal("2")),
                    "null"));

    private static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("basistarief", GEHEEL), attribuut("tarief", GEHEEL)),
                    List.of("aangemeld", "erkend")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("beloning", GEHEEL), attribuut("vergoeding", GEHEEL)),
                    List.of("toegewezen", "onbeheerd", "afwijkend", "begeleid", "verdacht", "geprijsd"))),
            List.of(TmsDocumenten.feittype("vinding", "vondst", "Vondst", "vinder", "Jutter")));

    @Test void een_steekproef_van_volgordes_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.steekproef(REGELS, 20261008L, 20)) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, volgorde), zaak(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(22, volgordes);
    }

    private static JsonNode zaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("J1", "Jutter", Map.of("basistarief", "4"), List.of("aangemeld"), Map.of()),
                        instantie("J2", "Jutter", Map.of("basistarief", "4"), List.of(), Map.of()),
                        instantie("V1", "Vondst", Map.of(), List.of(), Map.of("vinder", "J1")),
                        instantie("V2", "Vondst", Map.of(), List.of(), Map.of("vinder", "J2")),
                        instantie("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(verwacht("J1", "erkend", true), waarde("J1", "tarief", "5"),
                        verwacht("J2", "erkend", false), waarde("J2", "tarief", null),
                        verwacht("V1", "toegewezen", true), verwacht("V1", "onbeheerd", false),
                        verwacht("V1", "afwijkend", false), verwacht("V1", "begeleid", true),
                        verwacht("V1", "verdacht", false), verwacht("V1", "geprijsd", true),
                        waarde("V1", "beloning", "10"), waarde("V1", "vergoeding", "8"),
                        verwacht("V2", "toegewezen", false), verwacht("V2", "onbeheerd", true),
                        verwacht("V2", "afwijkend", true), verwacht("V2", "begeleid", false),
                        verwacht("V2", "verdacht", true), verwacht("V2", "geprijsd", true),
                        waarde("V2", "beloning", "0"), waarde("V2", "vergoeding", "8"),
                        verwacht("V3", "toegewezen", false), verwacht("V3", "onbeheerd", true),
                        verwacht("V3", "afwijkend", true), verwacht("V3", "begeleid", false),
                        verwacht("V3", "verdacht", false), verwacht("V3", "geprijsd", false),
                        waarde("V3", "beloning", "0"), waarde("V3", "vergoeding", "0")));
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
