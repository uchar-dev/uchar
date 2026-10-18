package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathCheck;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.predicate;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reads over a role on the exec-model branch where the link is absent (RUNTIME-SEMANTICS ch. 5.8). A Vondst
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
class TmsAbsentLinkWitnessTest {

    private static final String RATE = pathRead("tarief", "Jutter", "vinding", "vinder");
    private static final String BASE_RATE = pathRead("basistarief", "Jutter", "vinding", "vinder");

    private static final List<String> RULES = List.of(
            characteristicRule("erkenning", "Jutter", "erkend", check("aangemeld", false)),
            computingRule("tarief", "BEREKENING", "Jutter", "tarief",
                    binary("plus", ownRead("basistarief", "Jutter"), number("1")), check("erkend", false)),
            characteristicRule("toewijzing", "Vondst", "toegewezen", predicate("filled", RATE)),
            characteristicRule("onbeheerd", "Vondst", "onbeheerd", predicate("empty", RATE)),
            characteristicRule("afwijking", "Vondst", "afwijkend", comparison("notEqual", RATE, number("5"))),
            characteristicRule("begeleiding", "Vondst", "begeleid", pathCheck("erkend", false, "vinding", "vinder")),
            characteristicRule("verdenking", "Vondst", "verdacht", pathCheck("erkend", true, "vinding", "vinder")),
            characteristicRule("prijzing", "Vondst", "geprijsd", predicate("filled", BASE_RATE)),
            computingRule("beloning", "BEREKENING", "Vondst", "beloning", binary("times", RATE, number("2")), "null"),
            computingRule("vergoeding", "BEREKENING", "Vondst", "vergoeding", binary("times", BASE_RATE, number("2")),
                    "null"));

    private static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Jutter", List.of(attribute("basistarief", WHOLE), attribute("tarief", WHOLE)),
                    List.of("aangemeld", "erkend")),
            TmsDocuments.objectType("Vondst", List.of(attribute("beloning", WHOLE), attribute("vergoeding", WHOLE)),
                    List.of("toegewezen", "onbeheerd", "afwijkend", "begeleid", "verdacht", "geprijsd"))),
            List.of(TmsDocuments.factType("vinding", "vondst", "Vondst", "vinder", "Jutter")));

    @Test void a_sample_of_orders_of_the_rules_in_the_document() throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.sample(RULES, 20261008L, 20)) {
            List<String> names = names(order);
            assertNull(CaseRunner.playCase(TmsDocuments.document(MODEL, order), caseDocument(),
                    DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
            orders++;
        }
        assertEquals(22, orders);
    }

    private static JsonNode caseDocument() throws Exception {
        return TmsDocuments.caseDocument(
                List.of(instance("J1", "Jutter", Map.of("basistarief", "4"), List.of("aangemeld"), Map.of()),
                        instance("J2", "Jutter", Map.of("basistarief", "4"), List.of(), Map.of()),
                        instance("V1", "Vondst", Map.of(), List.of(), Map.of("vinder", "J1")),
                        instance("V2", "Vondst", Map.of(), List.of(), Map.of("vinder", "J2")),
                        instance("V3", "Vondst", Map.of(), List.of(), Map.of())),
                List.of(expected("J1", "erkend", true), value("J1", "tarief", "5"),
                        expected("J2", "erkend", false), value("J2", "tarief", null),
                        expected("V1", "toegewezen", true), expected("V1", "onbeheerd", false),
                        expected("V1", "afwijkend", false), expected("V1", "begeleid", true),
                        expected("V1", "verdacht", false), expected("V1", "geprijsd", true),
                        value("V1", "beloning", "10"), value("V1", "vergoeding", "8"),
                        expected("V2", "toegewezen", false), expected("V2", "onbeheerd", true),
                        expected("V2", "afwijkend", true), expected("V2", "begeleid", false),
                        expected("V2", "verdacht", true), expected("V2", "geprijsd", true),
                        value("V2", "beloning", "0"), value("V2", "vergoeding", "8"),
                        expected("V3", "toegewezen", false), expected("V3", "onbeheerd", true),
                        expected("V3", "afwijkend", true), expected("V3", "begeleid", false),
                        expected("V3", "verdacht", false), expected("V3", "geprijsd", false),
                        value("V3", "beloning", "0"), value("V3", "vergoeding", "0")));
    }

    private static List<String> names(List<String> rules) throws Exception {
        List<String> out = new ArrayList<>();
        for (String r : rules) out.add(TmsDocuments.JSON.readTree(r).get("name").asText());
        return out;
    }
}
