package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A characteristic written on one type of a subtype chain and read on another, on the exec-model branch. The
 * branch keys a characteristic on the type that declares it, so a derivation and a check meet on one key whichever type
 * of the chain each names. Jutter declares erkend, vergund and onvergund; Strandjutter, a subtype, declares
 * strandbevoegd. First the writer on the supertype and the negated reader on the subtype: "Een Jutter is vergund indien
 * hij erkend is", "Een Strandjutter is strandbevoegd indien hij niet vergund is". Then the writer on the subtype and the
 * negated reader on the supertype: "Een Strandjutter is vergund indien hij erkend is", "Een Jutter is onvergund indien
 * hij niet vergund is", where an erkende Jutter that is no Strandjutter is not vergund and so onvergund. Every order of
 * the two rules.
 *
 * <p>Green on its first run (2026-10-08): the keying on the declaring type was in place before this witness was
 * written. <b>Red by mutation</b> (measured the same day), with the key taken from the type the rule or the check names
 * instead of the declaring type: the writer and the reader no longer met, the reader took vergund for input that no
 * bean carries, and the two rows answered "case: kenmerk strandbevoegd: verwacht afwezig, werkelijk aanwezig" and "case:
 * kenmerk onvergund: verwacht afwezig, werkelijk aanwezig".
 */
class TmsSubtypeWitnessTest {

    @Test void writer_on_the_supertype_reader_on_the_subtype() throws Exception {
        List<String> rules = List.of(regel("vergund indien erkend", "Jutter", "vergund", check("erkend", false)),
                regel("strandbevoegd indien niet vergund", "Strandjutter", "strandbevoegd", check("vergund", true)));
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("S1", "Strandjutter", "erkend"), instance("S2", "Strandjutter", null),
                        instance("J1", "Jutter", "erkend")),
                List.of(expected("S1", "vergund", true), expected("S1", "strandbevoegd", false),
                        expected("S2", "vergund", false), expected("S2", "strandbevoegd", true),
                        expected("J1", "vergund", true)));
        everyOrder(rules, caseDocument);
    }

    @Test void writer_on_the_subtype_reader_on_the_supertype() throws Exception {
        List<String> rules = List.of(regel("vergund indien erkend", "Strandjutter", "vergund", check("erkend", false)),
                regel("onvergund indien niet vergund", "Jutter", "onvergund", check("vergund", true)));
        JsonNode caseDocument = TmsDocuments.caseDocument(
                List.of(instance("J1", "Jutter", "erkend"), instance("S1", "Strandjutter", "erkend"),
                        instance("S2", "Strandjutter", null)),
                List.of(expected("J1", "vergund", false), expected("J1", "onvergund", true),
                        expected("S1", "vergund", true), expected("S1", "onvergund", false),
                        expected("S2", "vergund", false), expected("S2", "onvergund", true)));
        everyOrder(rules, caseDocument);
    }

    private static void everyOrder(List<String> rules, JsonNode caseDocument) throws Exception {
        for (List<String> order : TmsDocuments.permutations(rules)) {
            Path ir = TmsDocuments.document(MODEL, order);
            List<String> names = new ArrayList<>();
            for (String r : order) names.add(TmsDocuments.JSON.readTree(r).get("name").asText());
            assertNull(CaseRunner.playCase(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + names);
        }
    }

    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[],"
            + "\"characteristics\":[" + characteristic("erkend") + "," + characteristic("vergund") + "," + characteristic("onvergund") + "]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Strandjutter\",\"animate\":true,\"plural\":\"Strandjutters\","
            + "\"supertype\":\"Jutter\",\"attributes\":[],\"characteristics\":[" + characteristic("strandbevoegd") + "]}],"
            + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
            + "\"dayKinds\":[]}";

    private static String characteristic(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    private static String regel(String naam, String type, String targetName, String condition) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + targetName + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"Een " + type + " is " + targetName + "\"}}";
        return IrPath.regel(naam, "KENMERKTOEKENNING", target, IrPath.literal("waar", "{\"node\":\"Boolean\"}", "waar"),
                condition);
    }

    private static String instance(String label, String type, String characteristic) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{},\"roles\":{},"
                + "\"characteristics\":[" + (characteristic == null ? "" : "\"" + characteristic + "\"") + "]}";
    }
}
