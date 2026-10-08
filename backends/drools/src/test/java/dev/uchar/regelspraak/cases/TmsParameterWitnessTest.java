package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.parameterlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Two parameters of one Java type on the truth-maintenance branch, each read by a rule of its own and both by one
 * condition and one value: "Een Vondst is zwaar indien zijn gewicht groter of gelijk is aan de drempel", "De toeslag van
 * een Vondst moet berekend worden als zijn gewicht plus het tarief", "Een Vondst is goedkoop indien het tarief kleiner
 * is dan de drempel" and "De korting van een Vondst moet berekend worden als het tarief plus de drempel". Drempel 10,
 * tarief 3. V1 (gewicht 12): zwaar, toeslag 15, goedkoop, korting 13. V2 (gewicht 5): not zwaar, toeslag 8, goedkoop,
 * korting 13. Every order of the four rules in the document, and the exec-model branch on the document. The golden
 * {@code jutterswet-aanvoergeld} has this shape with two parameters of which each rule reads one.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-08 on the branch's fourth step, which bound each parameter
 * through a {@code from} over the subject and the parameter's global): every read of either parameter returned the
 * drempel, "volgorde [zwaar bij drempel, toeslag van een vondst, goedkoop bij tarief, korting van een vondst] ==&gt;
 * expected: &lt;null&gt; but was: &lt;case: attribuut toeslag: verwacht 15, werkelijk 22; kenmerk goedkoop: verwacht
 * aanwezig, werkelijk afwezig; attribuut korting: verwacht 13, werkelijk 20; attribuut toeslag: verwacht 8, werkelijk
 * 15; kenmerk goedkoop: verwacht aanwezig, werkelijk afwezig; attribuut korting: verwacht 13, werkelijk 20&gt;". A
 * probe split it: a document with the toeslag rule alone, and one with the korting rule alone, played right; the
 * toeslag rule followed by the zwaar rule read the tarief in both, "kenmerk zwaar: verwacht afwezig, werkelijk
 * aanwezig". The exec-model row was green from the start, on the three rules it carries.
 */
class TmsParameterWitnessTest {

    private static final List<String> REGELS = List.of(
            kenmerkregel("zwaar bij drempel", "Vondst", "zwaar",
                    vergelijking("greaterOrEqual", lezing("gewicht", "Vondst"), parameterlezing("drempel"))),
            rekenregel("toeslag van een vondst", "BEREKENING", "Vondst", "toeslag",
                    binair("plus", lezing("gewicht", "Vondst"), parameterlezing("tarief")), "null"),
            kenmerkregel("goedkoop bij tarief", "Vondst", "goedkoop",
                    vergelijking("less", parameterlezing("tarief"), parameterlezing("drempel"))),
            rekenregel("korting van een vondst", "BEREKENING", "Vondst", "korting",
                    binair("plus", parameterlezing("tarief"), parameterlezing("drempel")), "null"));

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("gewicht", GEHEEL), attribuut("toeslag", GEHEEL), attribuut("korting", GEHEEL)),
            List.of("zwaar", "goedkoop"))), List.of(), List.of("drempel", "tarief"));

    @Test void elke_volgorde_van_de_regels_in_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = namen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, volgorde), zaak(true),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    /** Without the rule that compares the two parameters, which the exec-model branch refuses ("regel (voorwaarde
     *  buiten Phase-B scope): goedkoop bij tarief"). */
    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        List<String> regels = List.of(REGELS.get(0), REGELS.get(1), REGELS.get(3));
        assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, regels), zaak(false),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The case; {@code goedkoop} says whether the document carries the rule that compares the two parameters. */
    private static JsonNode zaak(boolean goedkoop) throws Exception {
        List<String> verwachtingen = new ArrayList<>(List.of(verwacht("V1", "zwaar", true), waarde("V1", "toeslag", "15"),
                waarde("V1", "korting", "13"), verwacht("V2", "zwaar", false), waarde("V2", "toeslag", "8"),
                waarde("V2", "korting", "13"), consistent()));
        if (goedkoop) verwachtingen.addAll(List.of(verwacht("V1", "goedkoop", true), verwacht("V2", "goedkoop", true)));
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("gewicht", "5"), List.of(), Map.of())),
                Map.of("drempel", "10", "tarief", "3"), verwachtingen);
    }

    private static List<String> namen(List<String> regels) throws Exception {
        List<String> uit = new ArrayList<>();
        for (String r : regels) uit.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
        return uit;
    }
}
