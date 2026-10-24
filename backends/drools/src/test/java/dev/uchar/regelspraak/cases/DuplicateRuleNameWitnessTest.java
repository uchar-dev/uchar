package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.literal;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
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
 *
 * <p>A form whose name is the name of a helper rule another form's lowering builds ({@code R} beside
 * {@code R: fout in de voorwaarde}) is no second form of one name, so WF-020 lets it pass, and the model still holds two
 * rules of one name: the refusal is the same. The rows of the four helper rules hold it on both branches, with and
 * without readings. <b>Red before the fix</b> (measured 2026-10-24 on {@code e3985673}, card drl-rule-part): the build
 * failed in the lowering with "de regel 'R: fout in de voorwaarde' is gebouwd als CONDITION_FAULT en als OWN"
 * ({@code IllegalStateException}), before the refusal, which a consumer that catches the refusals does not catch.
 */
class DuplicateRuleNameWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** The refusal of the exec-model branch after the label of its branch, which names the package of its rules. */
    private static String sentence(String name) {
        return ": Duplicate rule name: " + name + " — twee regels van één naam in pakket "
                + TmsTranspiler.PKG + "; de KieBase houdt één regel per naam, zodat"
                + " de tweede de eerste zou vervangen (vormnamen zijn uniek, WF-020)";
    }

    @Test void the_exec_branch_refuses_two_rules_of_one_name_by_name() throws Exception {
        Path ir = document(rule("actief", ACTIEF) + "," + rule("actief", WEIGHT), "");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir));
        assertEquals("exec-model" + sentence("actief"), e.getMessage());
    }

    @Test void the_exec_branch_refuses_a_rule_and_a_consistency_of_one_name_by_name() throws Exception {
        Path ir = document(rule("actief", ACTIEF) + "," + rule("gewicht", WEIGHT), CONSISTENCY);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> playTms(ir));
        assertEquals("exec-model" + sentence("gewicht"), e.getMessage());
    }

    @Test void the_drl_text_branch_refuses_the_same_document_by_name() throws Exception {
        Path ir = document(rule("actief", ACTIEF) + "," + rule("actief", WEIGHT), "");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> play(ir, true));
        assertTrue(e.getMessage().contains("Duplicate rule name: actief"), e.getMessage());
    }

    @Test void two_names_play_in_both_branches() throws Exception {
        Path ir = document(rule("actief", ACTIEF) + "," + rule("gewicht", WEIGHT), "");
        assertNull(play(ir, false), "exec-model");
        assertNull(play(ir, true), "DRL-tekst");
    }

    /** Every build of {@code ir}, on both branches and with {@code readings}, is refused with DROOLS-055 by name. */
    private static void refusedOnEveryBranch(Path ir, boolean readings, String name) throws Exception {
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir, branch, readings), branch + ", lezingen " + readings);
            assertEquals(DroolsCode.DUPLICATE_RULE_NAME, e.code(), branch + ", lezingen " + readings);
            assertEquals("exec-model" + sentence(name), e.getMessage(), branch + ", lezingen " + readings);
        }
    }

    @Test void a_form_named_like_the_fault_rule_of_a_condition_is_refused_by_name() throws Exception {
        // "laat" steps a date in its condition, which can fault, so its lowering builds "laat: fout in de voorwaarde"
        String laat = characteristicRule("laat", "Jutter", "laat", comparison("later", binary("plus",
                ownRead("aanmelddatum", "Jutter"), ownRead("wachtdagen", "Jutter"), DATE), literal("2026-01-01", DATE)));
        Path ir = helperDocument(List.of(laat, characteristicRule("laat: fout in de voorwaarde", "Jutter", "gemeld",
                "null")), List.of());
        refusedOnEveryBranch(ir, false, "laat: fout in de voorwaarde");
        refusedOnEveryBranch(ir, true, "laat: fout in de voorwaarde");
        // the analysis alone, which a caller can build without the backend's refusals: the shared name has no origin,
        // and says so; every other rule has its own
        dev.uchar.regelspraak.loader.IrLoader.Positioned positioned =
                dev.uchar.regelspraak.loader.IrLoader.loadWithPositions(JSON.readTree(ir.toFile()));
        ClassLoader cl = dev.uchar.regelspraak.engine.BeanCompiler.compile(
                dev.uchar.regelspraak.schema.FactModelGenerator.generate(positioned.loaded().model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        TmsTranspiler t;
        try {
            t = TmsTranspiler.build(positioned, cl);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
        assertEquals(new TmsTranspiler.RuleOrigin("laat", TmsTranspiler.RuleOrigin.Kind.FORM,
                TmsTranspiler.RuleOrigin.Role.OWN, null, null), t.originOf("laat"));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> t.originOf("laat: fout in de voorwaarde"));
        assertTrue(e.getMessage().contains("de vormen laat en laat: fout in de voorwaarde"), e::getMessage);
    }

    @Test void a_form_named_like_the_channel_rule_of_a_uniqueness_is_refused_by_name() throws Exception {
        // the uniqueness "eenmalig jutternummer" puts its violation on the channel in "eenmalig jutternummer: kanaal"
        Path ir = helperDocument(List.of(characteristicRule("eenmalig jutternummer: kanaal", "Jutter", "gemeld",
                "null")), List.of(UNIQUENESS));
        refusedOnEveryBranch(ir, false, "eenmalig jutternummer: kanaal");
        refusedOnEveryBranch(ir, true, "eenmalig jutternummer: kanaal");
    }

    @Test void a_form_named_like_the_contribution_rule_of_a_fold_is_refused_by_name() throws Exception {
        // "vondstental" counts the vondsten of the Jutter, the first fold of the document, which the lowering numbers 0
        String count = aggregation("count", VONDSTEN, "Vondst", sourceRole("aanmelding", "vondst"), "null", "null",
                COUNTING, "het aantal vondsten van de Jutter");
        Path ir = helperDocument(List.of(computingRule("vondstental", "GELIJKSTELLING", "Jutter", "vondstental", count,
                "null"), characteristicRule("vondstental: bijdrage aan vouw 0", "Jutter", "gemeld", "null")), List.of());
        refusedOnEveryBranch(ir, false, "vondstental: bijdrage aan vouw 0");
        refusedOnEveryBranch(ir, true, "vondstental: bijdrage aan vouw 0");
    }

    @Test void a_form_named_like_a_reading_rule_is_refused_in_a_build_with_readings() throws Exception {
        // "actief" has a condition, so a build with readings reads it in "actief: lezing"; a build without has no such
        // rule, and plays
        Path ir = helperDocument(List.of(characteristicRule("actief", "Jutter", "actief", check("gemeld", false)),
                characteristicRule("actief: lezing", "Jutter", "laat", "null")), List.of());
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values())
            assertEquals(List.of("actief", "actief: lezing"),
                    DroolsBackend.build(ir, branch, false).coverage().stream().map(c -> c.name()).toList(),
                    branch.toString());
        refusedOnEveryBranch(ir, true, "actief: lezing");
    }

    // ---------------- documents ----------------

    /** A Jutter with an aanmelddatum, wachtdagen, a jutternummer and a vondstental, and the characteristics laat, gemeld
     *  and actief, beside Vondsten it registered (aanmelding). */
    private static final String HELPER_MODEL = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(attribute("aanmelddatum", DATE),
                    attribute("wachtdagen", numberType(true, 0, unit("dagen"))),
                    attribute("jutternummer", TmsDocuments.WHOLE),
                    attribute("vondstental", COUNTING)), List.of("laat", "gemeld", "actief")),
            objectType("Vondst", List.of(attribute("taxatie", TmsDocuments.WHOLE)), List.of())),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")));

    private static final String VONDSTEN = "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\","
            + "\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}";

    /** The uniqueness "eenmalig jutternummer": "De jutternummers van alle Jutters moeten uniek zijn". */
    private static final String UNIQUENESS = "{\"node\":\"Uniqueness\",\"name\":\"eenmalig jutternummer\","
            + "\"versions\":[{\"sources\":[{\"node\":\"Source\",\"objectType\":\"Jutter\",\"keys\":[\"jutternummer\"]}],"
            + "\"validity\":{\"node\":\"Validity\"},\"source\":{\"snippet\":\"De jutternummers van alle Jutters moeten"
            + " uniek zijn\"}}]}";

    /** The document of {@link #HELPER_MODEL} with {@code rules} and {@code uniquenesses}, each in its order. */
    private static Path helperDocument(List<String> rules, List<String> uniquenesses) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + HELPER_MODEL + ",\"rules\":{\"rules\":["
                + String.join(",", rules) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[" + String.join(",", uniquenesses) + "],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("hulpregelnaam", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

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
    private static final String CONSISTENCY = "{\"node\":\"Consistency\",\"name\":\"gewicht\",\"versions\":[{\"subject\":"
            + "\"Jutter\",\"assertion\":{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":{\"node\":\"AttributeReference\","
            + "\"attribute\":\"gewicht\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"Het gewicht van een Jutter\"}},"
            + "\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"0\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"0\"}},"
            + "\"source\":{\"snippet\":\"Het gewicht van een Jutter moet groter zijn dan 0\"}},\"validity\":{\"node\":\"Validity\"},"
            + "\"source\":{\"snippet\":\"Het gewicht van een Jutter moet groter zijn dan 0\",\"line\":12}}]}";

    private static String rule(String name, String version) {
        return "{\"node\":\"Rule\",\"name\":\"" + name + "\",\"versions\":[" + version + "]}";
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
