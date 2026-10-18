package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two forms of one name: "Regel actief: Een Jutter is actief indien zijn startdag gevuld is" beside "Regel actief: Het
 * gewicht van een Jutter moet gesteld worden op 3". Form names are unique (WF-020); the frontend and the verifier refuse
 * such a document, so it reaches this backend only written by hand, and a consumer that builds a document itself can
 * hand it one. ref and clingo play both rules (`PASS (2 expectations)`). The KieBase holds one rule per name in a
 * package, so the exec-model branch must refuse the document by name before it builds one, as the DRL-text branch's
 * compiler does ("Duplicate rule name: actief"). The rows of that branch pin its sentence with the label of the
 * branch, {@code exec-model}. Every document is written here, on Jutterswet material.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-02 on the backend of {@code 70a60dc3}): the exec-model branch played
 * the document and answered "kenmerk actief: verwacht aanwezig, werkelijk afwezig" — the second rule had replaced the
 * first; beside a consistency of the rule's name it answered "attribuut gewicht: verwacht 3, werkelijk leeg".
 */
class DuplicateRuleNameWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The refusal of the exec-model branch after the label of its branch, which names the package of its rules. */
    private static String sentence(String naam) {
        return ": Duplicate rule name: " + naam + " — twee regels van één naam in pakket "
                + dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.PKG + "; de KieBase houdt één regel per naam, zodat"
                + " de tweede de eerste zou vervangen (vormnamen zijn uniek, WF-020)";
    }

    @Test void two_rules_of_a_name_refuses_the_named_exec_branch() throws Exception {
        Path ir = document(regel("actief", ACTIEF) + "," + regel("actief", WEIGHT), "");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir));
        assertEquals("exec-model" + sentence("actief"), e.getMessage());
    }

    @Test void a_rule_and_a_consistency_of_a_name_refuses_the_named_exec_branch() throws Exception {
        Path ir = document(regel("actief", ACTIEF) + "," + regel("gewicht", WEIGHT), CONSISTENTIE);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir));
        assertEquals("exec-model" + sentence("gewicht"), e.getMessage());
    }

    @Test void the_drl_text_branch_refuses_the_same_named_document() throws Exception {
        Path ir = document(regel("actief", ACTIEF) + "," + regel("actief", WEIGHT), "");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> play(ir, true));
        assertTrue(e.getMessage().contains("Duplicate rule name: actief"), e.getMessage());
    }

    @Test void two_names_play_in_both_branches() throws Exception {
        Path ir = document(regel("actief", ACTIEF) + "," + regel("gewicht", WEIGHT), "");
        assertNull(play(ir, false), "exec-model");
        assertNull(play(ir, true), "DRL-tekst");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,"
            + "\"plural\":\"Jutters\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"startdag\",\"datatype\":{\"node\":"
            + "\"Date\",\"precision\":\"day\"}},{\"node\":\"Attribute\",\"name\":\"gewicht\",\"datatype\":" + WHOLE + "}],"
            + "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"actief\",\"kind\":\"adjectival\"}]}],"
            + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
            + "\"dayKinds\":[]}";

    /** "Een Jutter is actief indien zijn startdag gevuld is". */
    private static final String ACTIEF = "{\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\","
            + "\"attribute\":\"actief\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Een Jutter is actief\"}},\"value\":"
            + "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},"
            + "\"condition\":{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":{\"node\":\"AttributeReference\",\"attribute\":"
            + "\"startdag\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn startdag\"}},\"negated\":false,\"digits\":0,"
            + "\"source\":{\"snippet\":\"zijn startdag gevuld is\"}},\"initialisation\":false}";

    /** "Het gewicht van een Jutter moet gesteld worden op 3". */
    private static final String WEIGHT = "{\"validity\":{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\","
            + "\"attribute\":\"gewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het gewicht van een Jutter\"}},"
            + "\"value\":{\"node\":\"Literal\",\"value\":{\"number\":\"3\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"3\"}},"
            + "\"initialisation\":false,\"presentation\":\"equation\"}";

    /** The consistency "gewicht": "Het gewicht van een Jutter moet groter zijn dan 0". */
    private static final String CONSISTENTIE = "{\"node\":\"Consistency\",\"name\":\"gewicht\",\"versions\":[{\"subject\":"
            + "\"Jutter\",\"assertion\":{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":{\"node\":\"AttributeReference\","
            + "\"attribute\":\"gewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het gewicht van een Jutter\"}},"
            + "\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"0\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"0\"}},"
            + "\"source\":{\"snippet\":\"Het gewicht van een Jutter moet groter zijn dan 0\"}},\"validity\":{\"node\":\"Validity\"},"
            + "\"source\":{\"snippet\":\"Het gewicht van een Jutter moet groter zijn dan 0\",\"line\":12}}]}";

    private static String regel(String naam, String version) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[" + version + "]}";
    }

    private static Path document(String rules, String consistencies) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + rules + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[" + consistencies
                + "],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],"
                + "\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("dubbele-regelnaam", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    // ---------------- the case ----------------

    /** J1 with a startdag: actief, and gewicht 3. */
    private static final String CASE = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[{\"label\":"
            + "\"J1\",\"objectType\":\"Jutter\",\"attributes\":{\"startdag\":{\"date\":\"2026-03-01\"}},\"roles\":{},"
            + "\"characteristics\":[]}],\"parameters\":{}},\"expected\":[{\"kind\":\"characteristicPresent\",\"label\":\"J1\","
            + "\"characteristic\":\"actief\"},{\"kind\":\"attributeValue\",\"label\":\"J1\",\"attribute\":\"gewicht\",\"value\":"
            + "{\"number\":\"3\"}}]}";

    /** One branch's verdict on the case: null when every expectation holds. */
    private static String play(Path ir, boolean viaDrl) throws Exception {
        return CaseRunner.playCase(ir, JSON.readTree(CASE), viaDrl);
    }

    private static String playTms(Path ir) throws Exception {
        return CaseRunner.playCase(ir, JSON.readTree(CASE), DroolsBackend.Branch.EXEC_MODEL);
    }
}
