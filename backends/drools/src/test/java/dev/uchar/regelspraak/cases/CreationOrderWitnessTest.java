package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An object creation that reads a value another form writes, where that form fires later on the agenda: a defaults group
 * (added after every creation, at a salience of 0 or below), a rule the stratification places below the others (a
 * negated characteristic), a creation declared after it, and a fact creation (added after every object creation).
 * RUNTIME-SEMANTICS ch. 6.1: a creation reads stratum-strict, so it evaluates strictly above the producers of what its
 * condition and its toekenningen read; it mints once, so a value that arrives after it is never seen. Every document
 * here is authored in this class on Jutterswet material, each one measured form with the values {@code ref} and clingo
 * answer. The creation is "Een Jutter heeft een afrekening met telling gelijk aan …", read back on the Jutter.
 *
 * <p>Situation (each case, unless a row says otherwise): J1 with V1 (gewicht 4), V2 (7) and V3 (no gewicht); J2 without
 * vondsten; J3 with only V4 (no gewicht); J4 with V5 (9).
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of main, exec-model branch): the creation fired
 * at the default salience after the ordinary rules and before the writer, and wrote what it read then: leeg where a
 * defaults group writes ({@code verwacht 7, werkelijk leeg}; {@code verwacht 15, werkelijk leeg}), 0 where a later
 * creation mints or a fact creation links ({@code verwacht 1, werkelijk 0}) and where a negation derives
 * ({@code verwacht 2, werkelijk 0}). One row is a flank, green before and after: an ordinary rule declared after the
 * creation fires before it all the same.
 */
class CreationOrderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_member_initialisation_reads_a_value_that_a_defaults_group_writes() throws Exception {
        // "… met telling gelijk aan zijn basis", with "De basis van een Jutter moet berekend worden als 2" and "Afwijking
        // van basis: opgegeven basis gaat voor …"; J1's opgegeven basis is 7. ref and clingo: 7, 2, 2 and 2.
        String group = group("basis", "Jutter", own("opgegeven basis", "Jutter"), number(2));
        String ir = document(List.of(creation(own("basis", "Jutter"))), List.of(READ_BACK), List.of(group), List.of());
        List<String> situation = new ArrayList<>(situation());
        situation.set(0, instance("J1", "Jutter", "\"opgegeven basis\":{\"number\":\"7\"}", ""));
        assertNull(play(ir, caseDocument(situation, count("7", "2", "2", "2"))), "de basis uit een defaultsgroep");
    }

    @Test void a_fold_over_a_member_that_a_defaults_group_writes() throws Exception {
        // "… met telling gelijk aan de som van het nettogewicht van alle vondsten van de Jutter", the nettogewicht by a
        // group: zijn gewicht min 1, or the opgegeven nettogewicht where it is filled (V1: 10). ref and clingo: J1 10 + 6
        // + (leeg min 1 = -1) = 15, J2 leeg, J3 -1 and J4 8.
        String group = group("nettogewicht", "Vondst", own("opgegeven nettogewicht", "Vondst"),
                binary("minus", own("gewicht", "Vondst"), number(1)));
        String ir = document(List.of(creation(fold("sum", "nettogewicht", null))), List.of(READ_BACK), List.of(group), List.of());
        List<String> situation = new ArrayList<>(situation());
        situation.set(4, situation.get(4).replace("\"attributes\":{", "\"attributes\":{\"opgegeven nettogewicht\":{\"number\":\"10\"},"));
        assertNull(play(ir, caseDocument(situation, count("15", null, "-1", "8"))), "een som over een lid uit een defaultsgroep");
    }

    @Test void a_fold_over_a_characteristic_that_a_negation_derives() throws Exception {
        // "… met telling gelijk aan het aantal vondsten van de Jutter die licht zijn", with "Een Vondst is licht indien hij
        // niet zwaar is" and "Een Vondst is zwaar indien zijn gewicht groter is dan 5": the stratification places the
        // negation below the other rules. ref and clingo: 2 (V1 and V3), 0, 1 and 0.
        String zwaar = characteristicRule("zwaar", "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + own("gewicht", "Vondst")
                + ",\"right\":" + number(5) + ",\"source\":{\"snippet\":\"zijn gewicht groter is dan 5\"}}");
        String licht = characteristicRule("licht", "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet zwaar is\"}}");
        String count = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}],\"elementType\":\"Vondst\","
                + "\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"licht\",\"negated\":false,\"source\":{\"snippet\":"
                + "\"licht zijn\"}},\"sourceRole\":" + roleBinding("JutterHeeftVondst", "vondst") + ",\"type\":" + COUNT_NUMBER
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter die licht zijn\"}}";
        String ir = document(List.of(creation(count)), List.of(READ_BACK, licht, zwaar), List.of(), List.of());
        assertNull(play(ir, caseDocument(situation(), count("2", "0", "1", "0"))), "een telling over een ontkend kenmerk");
    }

    @Test void a_count_over_what_a_later_creation_mints() throws Exception {
        // "… met telling gelijk aan het aantal netten van de Jutter", and after it "Een Jutter heeft een net met maas
        // gelijk aan 5". ref and clingo: 1 for every Jutter.
        String nets = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"netten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"netten van de Jutter\"}}],\"elementType\":\"Net\","
                + "\"sourceRole\":" + roleBinding("JutterHeeftNet", "net") + ",\"type\":" + COUNT_NUMBER
                + ",\"source\":{\"snippet\":\"het aantal netten van de Jutter\"}}";
        String objectCreationJson = "{\"node\":\"ObjectCreation\",\"name\":\"maak een net\",\"versions\":[{\"createdType\":\"Net\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"maas\",\"value\":" + number(5)
                + ",\"characteristic\":false}],\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + roleBinding("JutterHeeftNet", "net")
                + "}]}";
        String ir = document(List.of(creation(nets), objectCreationJson), List.of(READ_BACK), List.of(), List.of());
        assertNull(play(ir, caseDocument(situation(), count("1", "1", "1", "1"))), "een telling over de netten van een latere creatie");
    }

    @Test void a_count_over_a_relation_that_a_fact_creation_lays() throws Exception {
        // "… met telling gelijk aan het aantal gecontroleerde netten van de Jutter", with "Een gecontroleerd net van een
        // Jutter is een net van de Jutter"; each Jutter has one net. ref and clingo: 1 for every Jutter.
        String gecontroleerd = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"gecontroleerde netten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"gecontroleerde netten van de Jutter\"}}],"
                + "\"elementType\":\"Net\",\"sourceRole\":" + roleBinding("JutterControleertNet", "gecontroleerd net") + ",\"type\":"
                + COUNT_NUMBER + ",\"source\":{\"snippet\":\"het aantal gecontroleerde netten van de Jutter\"}}";
        String check = "{\"node\":\"FactCreation\",\"name\":\"controleer\",\"versions\":[{\"factType\":\"JutterControleertNet\","
                + "\"anchorRole\":\"controleur\",\"targetRole\":\"gecontroleerd net\",\"anchor\":\"Jutter\",\"path\":["
                + roleBinding("JutterHeeftNet", "net") + "],\"validity\":{\"node\":\"Validity\"}}]}";
        String ir = document(List.of(creation(gecontroleerd)), List.of(READ_BACK), List.of(), List.of(check));
        List<String> situation = new ArrayList<>(situation());
        for (int i = 1; i <= 4; i++) situation.add(instance("N" + i, "Net", "", "\"eigenaar\":[\"J" + i + "\"]"));
        assertNull(play(ir, caseDocument(situation, count("1", "1", "1", "1"))), "een telling over een relatie uit een feitcreatie");
    }

    // ---------------- the flank ----------------

    @Test void an_ordinary_rule_after_the_creation_fires_nevertheless_earlier() throws Exception {
        // "… met telling gelijk aan de som van het nettogewicht van alle vondsten van de Jutter", with "Het nettogewicht
        // van een Vondst moet berekend worden als zijn gewicht min 1" declared after the creation. ref and clingo: 8,
        // leeg, -1 and 8.
        String netto = "{\"node\":\"Rule\",\"name\":\"nettogewicht\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"nettogewicht\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Het nettogewicht van een Vondst\"}},\"value\":" + binary("minus", own("gewicht", "Vondst"), number(1))
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        String ir = document(List.of(creation(fold("sum", "nettogewicht", null))), List.of(READ_BACK, netto), List.of(), List.of());
        assertNull(play(ir, caseDocument(situation(), count("8", null, "-1", "8"))), "een som over een lid uit een latere regel");
    }

    // ---------------- documents ----------------

    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String COUNT_NUMBER = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";

    private static String attribute(String name) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + WHOLE + "}";
    }

    private static String characteristic(String name) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + name + "\",\"kind\":\"nominal\"}";
    }

    private static String objectType(String name, String plural, boolean animate, List<String> characteristics, String... members) {
        List<String> a = new ArrayList<>();
        for (String l : members) a.add(attribute(l));
        return "{\"node\":\"ObjectType\",\"name\":\"" + name + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", a) + "],\"characteristics\":[" + String.join(",", characteristics) + "]}";
    }

    private static String factType(String name, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + name + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\""
                + (manyPlural == null ? ",\"many\":false}" : ",\"plural\":\"" + manyPlural + "\",\"many\":true}") + "]}";
    }

    /** Jutter, Afrekening, Vondst and Net; one jutter has many vondsten, one afrekening, many netten and many
     *  gecontroleerde netten. */
    private static final String MODEL = "{\"objectTypes\":["
            + objectType("Jutter", "Jutters", true, List.of(), "telling", "basis", "opgegeven basis") + ","
            + objectType("Afrekening", "Afrekeningen", false, List.of(), "telling") + ","
            + objectType("Vondst", "Vondsten", false, List.of(characteristic("zwaar"), characteristic("licht")), "gewicht", "nettogewicht",
                    "opgegeven nettogewicht") + ","
            + objectType("Net", "Netten", false, List.of(), "maas")
            + "],\"factTypes\":["
            + factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + factType("JutterHeeftAfrekening", "afrekenaar", "Jutter", "afrekening", null, "Afrekening") + ","
            + factType("JutterHeeftNet", "eigenaar", "Jutter", "net", "netten", "Net") + ","
            + factType("JutterControleertNet", "controleur", "Jutter", "gecontroleerd net", "gecontroleerde netten", "Net")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String roleBinding(String factType, String role) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}";
    }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String binary(String op, String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + WHOLE
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A fold of kind {@code category} over the {@code member} of the vondsten of the Jutter; {@code filter} null for none. */
    private static String fold(String category, String member, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + category + "\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\""
                + member + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"het " + member + " van alle vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":"
                + roleBinding("JutterHeeftVondst", "vondst") + ",\"type\":" + WHOLE + ",\"source\":{\"snippet\":\"de som van het " + member
                + " van alle vondsten van de Jutter\"}}";
    }

    /** "Regel maak een afrekening: Een Jutter heeft een afrekening met telling gelijk aan &lt;waarde&gt;." */
    private static String creation(String value) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"maak een afrekening\",\"versions\":[{\"createdType\":\"Afrekening\","
                + "\"subject\":\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"telling\",\"value\":" + value
                + ",\"characteristic\":false}],\"validity\":{\"node\":\"Validity\"},\"connectingRole\":"
                + roleBinding("JutterHeeftAfrekening", "afrekening") + "}]}";
    }

    /** "De telling van een Jutter moet berekend worden als de telling van de afrekening van de Jutter." */
    private static final String READ_BACK = "{\"node\":\"Rule\",\"name\":\"lees telling terug\",\"versions\":[{\"validity\":"
            + "{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"telling\",\"subjectType\":\"Jutter\","
            + "\"source\":{\"snippet\":\"De telling van een Jutter\"}},\"value\":{\"node\":\"AttributeReference\",\"attribute\":"
            + "\"telling\",\"subjectType\":\"Afrekening\",\"source\":{\"snippet\":\"de telling van de afrekening van de Jutter\"},"
            + "\"path\":[" + roleBinding("JutterHeeftAfrekening", "afrekening") + "]},\"initialisation\":false,\"presentation\":"
            + "\"calculation\"}]}";

    /** "Een Vondst is &lt;kenmerk&gt; indien &lt;voorwaarde&gt;." */
    private static String characteristicRule(String characteristic, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + characteristic + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + characteristic + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is " + characteristic + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + condition + ",\"initialisation\":false}]}";
    }

    /** A defaults group on {@code member} of {@code type}: the opgave where it is filled, else {@code base}. */
    private static String group(String member, String type, String input, String base) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + member + "\",\"subject\":\"" + type + "\",\"attribute\":\"" + member
                + "\",\"defaults\":[{\"node\":\"Default\",\"justification\":{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":"
                + input + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"de opgave gevuld is\"}},\"consequence\":" + input
                + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
                + "\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + base + "}],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String document(List<String> creations, List<String> rules, List<String> groups, List<String> factCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creations) + "],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[" + String.join(",", factCreations) + "],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groups) + "]},"
                + "\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String vondst(String label, String weight, String jutter) {
        return instance(label, "Vondst", weight == null ? "" : "\"gewicht\":{\"number\":\"" + weight + "\"}",
                "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment: J1 to J4, then V1 to V5. */
    private static List<String> situation() {
        return List.of(instance("J1", "Jutter", "", ""), instance("J2", "Jutter", "", ""), instance("J3", "Jutter", "", ""),
                instance("J4", "Jutter", "", ""), vondst("V1", "4", "J1"), vondst("V2", "7", "J1"), vondst("V3", null, "J1"),
                vondst("V4", null, "J3"), vondst("V5", "9", "J4"));
    }

    /** The telling of J1 to J4, a null value leeg. */
    private static String count(String... values) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            String label = "J" + (i + 1);
            out.add(values[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"telling\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"telling\",\"value\":{\"number\":\""
                            + values[i] + "\"}}");
        }
        return String.join(",", out);
    }

    private static String caseDocument(List<String> instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, false);
    }

    private static String play(String ir, String caseDocument, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("creatie-volgorde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, drl);
    }
}
