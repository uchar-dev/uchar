package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A characteristic written on one type of a subtype chain and read on another, on the truth-maintenance branch. The
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

    @Test void schrijver_op_het_supertype_lezer_op_het_subtype() throws Exception {
        List<String> regels = List.of(regel("vergund indien erkend", "Jutter", "vergund", check("erkend", false)),
                regel("strandbevoegd indien niet vergund", "Strandjutter", "strandbevoegd", check("vergund", true)));
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("S1", "Strandjutter", "erkend"), instantie("S2", "Strandjutter", null),
                        instantie("J1", "Jutter", "erkend")),
                List.of(verwacht("S1", "vergund", true), verwacht("S1", "strandbevoegd", false),
                        verwacht("S2", "vergund", false), verwacht("S2", "strandbevoegd", true),
                        verwacht("J1", "vergund", true)));
        elkeVolgorde(regels, zaak);
    }

    @Test void schrijver_op_het_subtype_lezer_op_het_supertype() throws Exception {
        List<String> regels = List.of(regel("vergund indien erkend", "Strandjutter", "vergund", check("erkend", false)),
                regel("onvergund indien niet vergund", "Jutter", "onvergund", check("vergund", true)));
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("J1", "Jutter", "erkend"), instantie("S1", "Strandjutter", "erkend"),
                        instantie("S2", "Strandjutter", null)),
                List.of(verwacht("J1", "vergund", false), verwacht("J1", "onvergund", true),
                        verwacht("S1", "vergund", true), verwacht("S1", "onvergund", false),
                        verwacht("S2", "vergund", false), verwacht("S2", "onvergund", true)));
        elkeVolgorde(regels, zaak);
    }

    private static void elkeVolgorde(List<String> regels, JsonNode zaak) throws Exception {
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            List<String> namen = new ArrayList<>();
            for (String r : volgorde) namen.add(TmsDocumenten.JSON.readTree(r).get("name").asText());
            assertNull(CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.EXEC_MODEL), () -> "volgorde " + namen);
        }
    }

    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":[],"
            + "\"characteristics\":[" + kenmerk("erkend") + "," + kenmerk("vergund") + "," + kenmerk("onvergund") + "]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Strandjutter\",\"animate\":true,\"plural\":\"Strandjutters\","
            + "\"supertype\":\"Jutter\",\"attributes\":[],\"characteristics\":[" + kenmerk("strandbevoegd") + "]}],"
            + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
            + "\"dayKinds\":[]}";

    private static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"adjectival\"}";
    }

    private static String regel(String naam, String type, String doel, String voorwaarde) {
        String target = "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"Een " + type + " is " + doel + "\"}}";
        return IrPad.regel(naam, "KENMERKTOEKENNING", target, IrPad.literal("waar", "{\"node\":\"Boolean\"}", "waar"),
                voorwaarde);
    }

    private static String instantie(String label, String type, String kenmerk) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{},\"roles\":{},"
                + "\"characteristics\":[" + (kenmerk == null ? "" : "\"" + kenmerk + "\"") + "]}";
    }
}
