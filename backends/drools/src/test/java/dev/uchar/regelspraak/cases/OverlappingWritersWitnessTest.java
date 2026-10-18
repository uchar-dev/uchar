package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two conditional rules that write one attribute and whose conditions both hold on one instance ("De meldingsdatum
 * van een Vondst moet gesteld worden op zijn vinddatum indien zijn vinddatum gevuld is", and the same on its
 * aanspoeldatum). WF-079 refuses two unconditional writers only, so such a document is well-formed and the executors
 * answer it. RUNTIME-SEMANTICS ch. 5.1 reads every rule as an equation; two equations that demand different values
 * for one target have no fixpoint, and ch. 5.7 makes that divergence a fault. Both Drools branches must report it as
 * such, naming the writers, the target and the values they write, and never a cycle the document does not contain.
 * Where only one condition holds, or both writers write the same value, or the conditions exclude each other, the
 * document plays. Every document here is authored in this class on Jutterswet material, each one measured form of the
 * card's form space; {@code ref} faults on the same bodies and the ASP backend derives a named witness.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the jar of the claim): the exec-model branch ran into its fire
 * cap and reported {@code geen fixpunt (kringverwijzing?)}, a cause the document does not have; the DRL-text branch
 * fired each writer once and kept the value of the writer declared last, without a diagnostic ({@code PLAYED} where
 * that was the expected value, {@code verwacht 2026-03-04, werkelijk leeg} where the last writer evaluated leeg). The
 * three flank rows are green before and after.
 *
 * <p>Since card tms-omschakeling (2026-10-10) the exec-model branch derives with truth maintenance, and since card
 * drl-teksttak-op-truth-maintenance the DRL-text branch renders the same analysis. Each holds both writers' values as
 * facts and finds the divergence at the fixpoint, where its write-back throws the fault ({@code Schrijversconflict});
 * the destructive exec-model branch found it at its fire cap, and the destructive DRL-text lowering gave the same
 * sentence as its verdict.
 */
class OverlappingWritersWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_two_conditional_writers_on_a_type() throws Exception {
        // The card's R1: V with vinddatum 04-03-2026 and aanspoeldatum 01-03-2026. ref: no fixpoint, the two rules named.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String caseDocument = caseDocument(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", null)),
                dateExpected("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : BRANCHES)
            reportsWriters(play(ir, caseDocument, drl), branch(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van V, een Vondst");
    }

    @Test void r2_the_second_writer_on_a_subtype() throws Exception {
        // The card's R2: the second writer on the subtype Strandvondst, S a Strandvondst with both dates.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", attribuut("vinddatum", DATE),
                        attribuut("meldingsdatum", DATE)) + ","
                        + objecttype("Strandvondst", "Strandvondsten", "Vondst", "", attribuut("aanspoeldatum", DATE)),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Strandvondst", "aanspoeldatum"));
        String caseDocument = caseDocument(List.of(vondst("S", "Strandvondst", "2026-03-04", "2026-03-01", null)),
                dateExpected("S", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : BRANCHES)
            reportsWriters(play(ir, caseDocument, drl), branch(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "\"melding op vinddag\" schrijft 2026-03-04",
                    "\"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van S, een Strandvondst");
    }

    @Test void r1_on_the_red_route() throws Exception {
        // The red path of gate 8 replays the case on its own fire loop: the same sentence, as a FAULT.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        Path map = Files.createTempDirectory("overlappende-schrijvers");
        map.toFile().deleteOnExit();
        Path irPath = map.resolve("ir.json");
        Path casePath = map.resolve("case1.json");
        Files.writeString(irPath, ir);
        Files.writeString(casePath, "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + vondst("V", "Vondst", "2026-03-04", "2026-03-01", null) + "],\"parameters\":{}},\"expectedFault\":"
                + "{\"ref\":\"two writers of one target disagree\",\"clingo\":\"two writers of one target disagree\"}}");
        irPath.toFile().deleteOnExit();
        casePath.toFile().deleteOnExit();
        CaseRunner.RedResult r = CaseRunner.runRed(irPath, casePath);
        assertEquals("FAULT", r.status(), r.detail());
        reportsWriters(r.detail(), "rode route", "de regel \"melding op vinddag\" schrijft 2026-03-04",
                "de regel \"melding op aanspoeldag\" schrijft 2026-03-01", "in het attribuut meldingsdatum van V, een Vondst");
    }

    // ---------------- the form space ----------------

    @Test void three_writers_with_three_values() throws Exception {
        // Three writers, each with its own date. ref names all three; in the engine two of them can keep firing each
        // other while the third waits on the agenda, so a branch names at least two, each with its own value.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"),
                messageOn("melding op bergdag", "Vondst", "bergdatum"));
        String caseDocument = caseDocument(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", "2026-03-06")),
                dateExpected("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : BRANCHES) {
            String out = play(ir, caseDocument, drl);
            reportsWriters(out, branch(drl), "schrijvers van één doel geven verschillende waarden",
                    "in het attribuut meldingsdatum van V, een Vondst");
            long named = java.util.stream.Stream.of("de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01",
                    "de regel \"melding op bergdag\" schrijft 2026-03-06").filter(out::contains).count();
            assertTrue(named >= 2, branch(drl) + " noemt ten minste twee schrijvers met hun waarde: " + out);
        }
    }

    @Test void a_writer_that_computes_empty_opposite_one_that_writes_a_value() throws Exception {
        // Both conditions read the vinddatum; the second writer's value, the aanspoeldatum, is leeg, and leeg over a
        // value is a write (ch. 1.2g), so the two keep writing 04-03-2026 and leeg. ref faults; the ASP backend gave
        // 04-03-2026 before the fix, because a leeg value derives no atom there.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                regel("melding op aanspoeldag", "Vondst", "meldingsdatum", lees("Vondst", "aanspoeldatum"),
                        test("filled", "Vondst", "vinddatum"), "equation"));
        String caseDocument = caseDocument(List.of(vondst("V", "Vondst", "2026-03-04", null, null)),
                dateExpected("V", "meldingsdatum", "2026-03-04"));
        for (DroolsBackend.Branch drl : BRANCHES)
            reportsWriters(play(ir, caseDocument, drl), branch(drl), "twee schrijvers van één doel geven verschillende waarden",
                    "\"melding op vinddag\" schrijft 2026-03-04",
                    "\"melding op aanspoeldag\" schrijft leeg", "in het attribuut meldingsdatum van V, een Vondst");
    }

    @Test void an_equation_beside_a_calculation() throws Exception {
        // A number: "gesteld worden op zijn schatting" beside "berekend worden als zijn waarde min 5,00", schatting
        // 20,00 and waarde 50,00, so 20.00 and 45.00.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", attribuut("waarde", TWO_DECIMALS),
                        attribuut("schatting", TWO_DECIMALS), attribuut("vindersloon", TWO_DECIMALS)),
                regel("vindersloon naar schatting", "Vondst", "vindersloon", lees("Vondst", "schatting"),
                        test("filled", "Vondst", "schatting"), "equation"),
                regel("vindersloon naar waarde", "Vondst", "vindersloon", VALUE_MINUS_FIVE,
                        test("filled", "Vondst", "waarde"), "calculation"));
        String caseDocument = caseDocument(List.of(instance("V", "Vondst", "\"schatting\":{\"number\":\"20.00\"},\"waarde\":{\"number\":\"50.00\"}")),
                "{\"kind\":\"attributeValue\",\"label\":\"V\",\"attribute\":\"vindersloon\",\"value\":{\"number\":\"20.00\"}}");
        for (DroolsBackend.Branch drl : BRANCHES)
            reportsWriters(play(ir, caseDocument, drl), branch(drl), "\"vindersloon naar schatting\" schrijft 20.00",
                    "\"vindersloon naar waarde\" schrijft 45.00", "in het attribuut vindersloon van V, een Vondst");
    }

    @Test void the_writers_in_a_rule_group_are_named_after_their_rule() throws Exception {
        // The engine names a member of a rule group "<groep>: <regel>"; the report names the rule.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                List.of("{\"node\":\"RuleGroup\",\"name\":\"melding\",\"recursive\":false,\"members\":[\"melding op vinddag\","
                        + "\"melding op aanspoeldag\"],\"source\":{\"snippet\":\"melding\"}}"),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String caseDocument = caseDocument(List.of(vondst("V", "Vondst", "2026-03-04", "2026-03-01", null)),
                dateExpected("V", "meldingsdatum", "2026-03-01"));
        for (DroolsBackend.Branch drl : BRANCHES) {
            String out = play(ir, caseDocument, drl);
            reportsWriters(out, branch(drl), "de regel \"melding op vinddag\" schrijft 2026-03-04",
                    "de regel \"melding op aanspoeldag\" schrijft 2026-03-01");
            assertFalse(out.contains("\"melding: "), branch(drl) + " noemt de regel, niet de regelnaam van de engine: " + out);
        }
    }

    // ---------------- flanks: what keeps playing ----------------

    @Test void the_disjoint_form_plays() throws Exception {
        // "indien zijn aanspoeldatum gevuld is" beside "indien zijn aanspoeldatum leeg is": one writer per instance.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"),
                regel("melding op vinddag", "Vondst", "meldingsdatum", lees("Vondst", "vinddatum"),
                        test("empty", "Vondst", "aanspoeldatum"), "equation"));
        String caseDocument = caseDocument(List.of(vondst("V1", "Vondst", "2026-03-04", "2026-03-01", null),
                        vondst("V2", "Vondst", "2026-03-04", null, null), vondst("V3", "Vondst", null, "2026-03-01", null),
                        vondst("V4", "Vondst", null, null, "2026-03-06")),
                dateExpected("V1", "meldingsdatum", "2026-03-01"), dateExpected("V2", "meldingsdatum", "2026-03-04"),
                dateExpected("V3", "meldingsdatum", "2026-03-01"),
                "{\"kind\":\"attributeEmpty\",\"label\":\"V4\",\"attribute\":\"meldingsdatum\"}");
        for (DroolsBackend.Branch drl : BRANCHES) assertNull(play(ir, caseDocument, drl), branch(drl));
    }

    @Test void two_writers_with_the_same_value_play() throws Exception {
        // Both conditions hold and both values are 02-03-2026: both equations hold, the fixpoint exists.
        String ir = document(objecttype("Vondst", "Vondsten", null, "", DATES),
                messageOn("melding op vinddag", "Vondst", "vinddatum"),
                messageOn("melding op aanspoeldag", "Vondst", "aanspoeldatum"));
        String caseDocument = caseDocument(List.of(vondst("V", "Vondst", "2026-03-02", "2026-03-02", null)),
                dateExpected("V", "meldingsdatum", "2026-03-02"));
        for (DroolsBackend.Branch drl : BRANCHES) assertNull(play(ir, caseDocument, drl), branch(drl));
    }

    @Test void two_characteristic_assignments_play() throws Exception {
        // A characteristic is monotone (ch. 5.3): two attributions that both hold set it once.
        String ir = document(objecttype("Vondst", "Vondsten", null,
                        "{\"node\":\"Characteristic\",\"name\":\"meldplichtig\",\"kind\":\"adjectival\"}", DATES),
                characteristicRule("meldplicht na vinden", "Vondst", "meldplichtig", test("filled", "Vondst", "vinddatum")),
                characteristicRule("meldplicht na aanspoelen", "Vondst", "meldplichtig", test("filled", "Vondst", "aanspoeldatum")));
        String caseDocument = caseDocument(List.of(vondst("V1", "Vondst", "2026-03-04", "2026-03-01", null), vondst("V2", "Vondst", null, null, null)),
                "{\"kind\":\"characteristicPresent\",\"label\":\"V1\",\"characteristic\":\"meldplichtig\"}",
                "{\"kind\":\"characteristicAbsent\",\"label\":\"V2\",\"characteristic\":\"meldplichtig\"}");
        for (DroolsBackend.Branch drl : BRANCHES) assertNull(play(ir, caseDocument, drl), branch(drl));
    }

    // ---------------- assertions ----------------

    /** Both branches, which give the divergence as the fault their write-back throws at the fixpoint. */
    private static final List<DroolsBackend.Branch> BRANCHES = List.of(DroolsBackend.Branch.EXEC_MODEL,
            DroolsBackend.Branch.DRL_TEXT);

    private static String branch(DroolsBackend.Branch branch) {
        return branch == DroolsBackend.Branch.DRL_TEXT ? "DRL-teksttak" : "exec-model-tak";
    }

    /** The verdict names the divergence with each of {@code fragments}, and no cycle. */
    private static void reportsWriters(String out, String formName, String... fragments) {
        assertNotNull(out, formName + ": speelt, waar de twee vergelijkingen geen fixpunt hebben");
        assertTrue(out.contains("geen fixpunt"), formName + ": " + out);
        assertTrue(out.contains("divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)"), formName + ": " + out);
        assertFalse(out.contains("kringverwijzing"), formName + " noemt een kring die het document niet heeft: " + out);
        for (String f : fragments) assertTrue(out.contains(f), formName + " noemt '" + f + "': " + out);
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static final String DATES = String.join(",", attribuut("vinddatum", DATE), attribuut("aanspoeldatum", DATE),
            attribuut("bergdatum", DATE), attribuut("meldingsdatum", DATE));

    private static String objecttype(String naam, String plural, String supertype, String kenmerken, String... attributes) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\"" + (supertype == null ? "" : ",\"supertype\":\"" + supertype + "\"")
                + ",\"animate\":false,\"plural\":\"" + plural + "\",\"attributes\":[" + String.join(",", attributes)
                + "],\"characteristics\":[" + kenmerken + "]}";
    }

    private static String lees(String type, String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "zijn &lt;attribuut&gt; gevuld is" ({@code filled}) or "leeg is" ({@code empty}). */
    private static String test(String soort, String type, String attribuut) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + lees(type, attribuut) + ",\"negated\":false,"
                + "\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribuut + (soort.equals("filled") ? " gevuld is" : " leeg is") + "\"}}";
    }

    /** "zijn waarde min 5,00", as the frontend writes it. */
    private static final String VALUE_MINUS_FIVE = "{\"node\":\"Binary\",\"op\":\"minus\",\"left\":" + lees("Vondst", "waarde")
            + ",\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"5.00\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":"
            + "\"5,00\"}},\"type\":" + TWO_DECIMALS + ",\"source\":{\"snippet\":\"zijn waarde min 5,00\"}}";

    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde, String presentation) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"De " + doel + " van een " + type + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":false,\"presentation\":\"" + presentation + "\"}]}";
    }

    /** "De meldingsdatum van een &lt;type&gt; moet gesteld worden op zijn &lt;bron&gt; indien zijn &lt;bron&gt; gevuld is." */
    private static String messageOn(String naam, String type, String bron) {
        return regel(naam, type, "meldingsdatum", lees(type, bron), test("filled", type, bron), "equation");
    }

    private static String characteristicRule(String naam, String type, String kenmerk, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"Een " + type + " is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},"
                + "\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    private static String document(String objectTypes, String... rules) {
        return document(objectTypes, List.of(), rules);
    }

    private static String document(String objectTypes, List<String> groups, String... rules) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[" + objectTypes + "],\"factTypes\":[],\"parameters\":[],"
                + "\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},\"rules\":{\"rules\":["
                + String.join(",", rules) + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":["
                + String.join(",", groups) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{},"
                + "\"characteristics\":[]}";
    }

    private static String vondst(String label, String type, String vinddatum, String aanspoeldatum, String bergDate) {
        List<String> a = new ArrayList<>();
        if (vinddatum != null) a.add("\"vinddatum\":{\"date\":\"" + vinddatum + "\"}");
        if (aanspoeldatum != null) a.add("\"aanspoeldatum\":{\"date\":\"" + aanspoeldatum + "\"}");
        if (bergDate != null) a.add("\"bergdatum\":{\"date\":\"" + bergDate + "\"}");
        return instance(label, type, String.join(",", a));
    }

    private static String dateExpected(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\",\"value\":{\"date\":\""
                + iso + "\"}}";
    }

    private static String caseDocument(List<String> instanties, String... verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** One branch's verdict on one case: null when every expectation holds, and the message of the fault the replay
     *  throws, which on both branches carries a divergence at the fixpoint. */
    private static String play(String ir, String caseDocument, DroolsBackend.Branch branch) throws Exception {
        Path tmp = Files.createTempFile("overlappende-schrijvers", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        try {
            return CaseRunner.playCase(tmp, c, branch);
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
