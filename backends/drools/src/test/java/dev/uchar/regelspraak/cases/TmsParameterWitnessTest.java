package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.parameterRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocuments.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Two parameters of one Java type on the exec-model branch, each read by a rule of its own and both by one
 * condition and one value: "Een Vondst is zwaar indien zijn gewicht groter of gelijk is aan de drempel", "De toeslag van
 * een Vondst moet berekend worden als zijn gewicht plus het tarief", "Een Vondst is goedkoop indien het tarief kleiner
 * is dan de drempel" and "De korting van een Vondst moet berekend worden als het tarief plus de drempel". Drempel 10,
 * tarief 3. V1 (gewicht 12): zwaar, toeslag 15, goedkoop, korting 13. V2 (gewicht 5): not zwaar, toeslag 8, goedkoop,
 * korting 13. Every order of the four rules in the document, and every order of the three without the rule that
 * compares the two parameters, which give the same values but goedkoop. The reference interpreter and the ASP runner
 * give these values on both documents, and the verifier passes both (measured 2026-10-10). The golden
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
 *
 * <p>The row of three rules played the destructive exec-model branch, which refused the comparison of two parameters,
 * in the document's own order. At the switch-over (card tms-omschakeling, step 5a2) it went as a comparison of the
 * branch with itself; it was none, since the four rules are another document, and the lens on that step brought it
 * back on the exec-model branch, in every order.
 */
class TmsParameterWitnessTest {

    private static final List<String> RULES = List.of(
            characteristicRule("zwaar bij drempel", "Vondst", "zwaar",
                    comparison("greaterOrEqual", reading("gewicht", "Vondst"), parameterRead("drempel"))),
            computingRule("toeslag van een vondst", "BEREKENING", "Vondst", "toeslag",
                    binary("plus", reading("gewicht", "Vondst"), parameterRead("tarief")), "null"),
            characteristicRule("goedkoop bij tarief", "Vondst", "goedkoop",
                    comparison("less", parameterRead("tarief"), parameterRead("drempel"))),
            computingRule("korting van een vondst", "BEREKENING", "Vondst", "korting",
                    binary("plus", parameterRead("tarief"), parameterRead("drempel")), "null"));

    private static final String MODEL = TmsDocuments.model(List.of(TmsDocuments.objecttype("Vondst",
            List.of(attribuut("gewicht", WHOLE), attribuut("toeslag", WHOLE), attribuut("korting", WHOLE)),
            List.of("zwaar", "goedkoop"))), List.of(), List.of("drempel", "tarief"));

    @Test void every_order_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(RULES)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument(true),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(24, orders);
    }

    @Test void every_order_of_three_rules_without_the_comparison_of_the_parameters() throws Exception {
        List<String> three = List.of(RULES.get(0), RULES.get(1), RULES.get(3));
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(three)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument(false),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(6, orders);
    }

    /** The case; {@code cheap} says whether the document carries the rule that compares the two parameters. */
    private static JsonNode caseDocument(boolean cheap) throws Exception {
        List<String> verwachtingen = new ArrayList<>(List.of(verwacht("V1", "zwaar", true), waarde("V1", "toeslag", "15"),
                waarde("V1", "korting", "13"), verwacht("V2", "zwaar", false), waarde("V2", "toeslag", "8"),
                waarde("V2", "korting", "13"), consistent()));
        if (cheap) verwachtingen.addAll(List.of(verwacht("V1", "goedkoop", true), verwacht("V2", "goedkoop", true)));
        return TmsDocuments.caseDocument(
                List.of(instance("V1", "Vondst", Map.of("gewicht", "12"), List.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("gewicht", "5"), List.of(), Map.of())),
                Map.of("drempel", "10", "tarief", "3"), verwachtingen);
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
