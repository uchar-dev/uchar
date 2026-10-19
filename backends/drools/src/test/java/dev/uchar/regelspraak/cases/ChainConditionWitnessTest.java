package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A voorwaarde that reads through a CHAIN of to-one roles ("de afgeleide openingsdag van de haven van de jutter van
 * de Vondst"), and one that reads through one role inside a date computation ("zijn vinddag min 6 mnd later is dan de
 * vergunningsdag van de jutter van de Vondst"). {@code ref/} and the ASP backend answer both; the same reads in a
 * rule's VALUE played in the exec-model branch, and a voorwaarde over one role without a date computation played
 * since it got the far-side join (NavigatedConditionWitnessTest). Every document here is authored in this class
 * on Jutterswet material, each one measured form of the card's form space with the values {@code ref} and clingo
 * answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): every row
 * over a voorwaarde was refused in the generic sentence {@code transpiler SKIP: regel: <regel> — regel (voorwaarde
 * buiten Phase-B scope): <regel>}. The row whose second relation a fact creation sets later also fails with the chain
 * joined at its END object only, the one join a chain read in a rule's value had: the Jutter the fact creation
 * updates is then not in the rule's tuple, so the test is not run again ({@code attribuut uitkomst: verwacht
 * 2026-09-15, werkelijk leeg}); it holds the join per hop. The row over the VALUE was that same wrong answer on the
 * claim ({@code verwacht 2026-01-01, werkelijk leeg}): the value's chain now gets the join per hop too.
 */
class ChainConditionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_a_comparison_of_two_far_roles_sees_the_derived_date() throws Exception {
        // The card's R1: "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn vinddag later is
        // dan de afgeleide openingsdag van de haven van de jutter van de Vondst", with "De afgeleide openingsdag van een
        // Haven moet gesteld worden op zijn openingsdag" before it, and once more after it. ref and clingo: V
        // 15-09-2026, W leeg.
        String outcome = outcomeIf(comparison("later", own("vinddag"), viaHaven("afgeleide openingsdag")));
        String caseDocument = caseDocument(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                date("V", "2026-09-15") + ", " + empty("W"));
        assertNull(play(document(DERIVED_OPENINGSDAG, outcome), caseDocument), "de afleiding voor de lezer");
        assertNull(play(document(outcome, DERIVED_OPENINGSDAG), caseDocument), "de afleiding na de lezer");
        // the DRL-text branch renders the same analysis since card drl-teksttak-op-truth-maintenance, and plays both
        // orders; until then it refused the comparison ("TODO voorwaarde")
        assertNull(playDrl(document(DERIVED_OPENINGSDAG, outcome), caseDocument), "drl-tekst: de afleiding voor de lezer");
        assertNull(playDrl(document(outcome, DERIVED_OPENINGSDAG), caseDocument), "drl-tekst: de afleiding na de lezer");
    }

    @Test void r2_filled_two_far_roles_on_an_input_value() throws Exception {
        // The card's R2: "… indien de openingsdag van de haven van de jutter van de Vondst gevuld is". ref and clingo:
        // V 15-09-2026, W 15-12-2025.
        String ir = document(DERIVED_OPENINGSDAG, outcomeIf(predicate("filled", viaHaven("openingsdag"))));
        assertNull(play(ir, caseDocument(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                date("V", "2026-09-15") + ", " + date("W", "2025-12-15"))), "gevuld over twee rollen");
    }

    @Test void r3_a_date_arithmetic_on_the_own_side_a_far_role() throws Exception {
        // The card's R3: "… indien zijn vinddag min 6 mnd later is dan de vergunningsdag van de jutter van de Vondst".
        // ref and clingo: V 15-09-2026 (15-03-2026 is later than 01-03-2026), W leeg (15-06-2025 is not).
        String ir = document(DERIVED_OPENINGSDAG, outcomeIf(comparison("later",
                months("minus", own("vinddag"), "6", "zijn vinddag min 6 mnd"), viaJutter("vergunningsdag"))));
        assertNull(play(ir, caseDocument(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                date("V", "2026-09-15") + ", " + empty("W"))), "een datumrekensom een rol ver");
    }

    // ---------------- the flank and the join per hop ----------------

    @Test void without_relation_on_the_second_hop_reads_the_chain_empty() throws Exception {
        // V2's jutter has no haven, V3 has no jutter: RUNTIME-SEMANTICS ch. 1.2, a failed to-one navigation is leeg.
        // "… op 01-01-2026 indien de afgeleide openingsdag van de haven van de jutter van de Vondst leeg is", the
        // derivation first (with the reader first the test fires on the value before its derivation, the recorded
        // bound of the leeg test); and R1 over the same Vondsten. ref and clingo: the leeg test V1 leeg, V2 and V3
        // 01-01-2026; R1 V1 15-09-2026, V2 and V3 leeg.
        List<String> situation = List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                jutter("J2", "2026-03-01", null), vondst("V1", "2026-09-15", "J"), vondst("V2", "2026-09-15", "J2"),
                vondst("V3", "2026-09-15", null));
        String emptyTest = regel("uitkomst", "Vondst", "uitkomst", dateLiteral("2026-01-01"),
                predicate("empty", viaHaven("afgeleide openingsdag")));
        assertNull(play(document(DERIVED_OPENINGSDAG, emptyTest), caseDocument(situation,
                empty("V1") + ", " + date("V2", "2026-01-01") + ", " + date("V3", "2026-01-01"))), "de leeg-toets");
        String r1 = outcomeIf(comparison("later", own("vinddag"), viaHaven("afgeleide openingsdag")));
        assertNull(play(document(DERIVED_OPENINGSDAG, r1), caseDocument(situation,
                date("V1", "2026-09-15") + ", " + empty("V2") + ", " + empty("V3"))), "de vergelijking");
    }

    @Test void the_second_relation_that_a_fact_creation_later_lays() throws Exception {
        // R2's test, with the haven of J not given but created: "Een havenjutter van een Haven is een geworven jutter
        // van de kustwacht van de Haven". The fact creation sets J's haven and updates J after V matched; only a join
        // on the Jutter runs the test again. ref and clingo: V 15-09-2026, X (its jutter has no werver) leeg.
        String ir = document(List.of(outcomeIf(predicate("filled", viaHaven("openingsdag"))), DERIVED_OPENINGSDAG),
                WERVING);
        assertNull(play(ir, caseDocument(WERVING_SITUATION, date("V", "2026-09-15") + ", " + empty("X"))), "de haven komt uit de werving");
    }

    @Test void the_value_over_the_chain_sees_the_second_relation_that_a_fact_creation_later_lays() throws Exception {
        // The same fact creation under a rule whose VALUE reads the chain: "De uitkomst van een Vondst moet gesteld
        // worden op de openingsdag van de haven van de jutter van de Vondst". The value's far-side join on the reached
        // Haven alone does not see J's haven being set. ref and clingo: V 01-01-2026, X leeg. Measured 2026-10-01:
        // red on the transpiler of the fix of the voorwaarde (attribuut uitkomst: verwacht 2026-01-01, werkelijk leeg),
        // as on the claim.
        String ir = document(List.of(regel("uitkomst", "Vondst", "uitkomst", viaHaven("openingsdag"), null), DERIVED_OPENINGSDAG),
                WERVING);
        assertNull(play(ir, caseDocument(WERVING_SITUATION, date("V", "2026-01-01") + ", " + empty("X"))), "de haven komt uit de werving");
    }

    // ---------------- further forms of (a) ----------------

    @Test void three_far_roles_with_a_date_arithmetic() throws Exception {
        // "… indien zijn vinddag later is dan de afgeleide sluitingsdag van de kustwacht van de haven van de jutter van
        // de Vondst plus 3 mnd", the derivation of the sluitingsdag after the reader. ref and clingo: V 15-09-2026
        // (later than 01-09-2026), W leeg (15-05-2026), U leeg (its haven has no kustwacht).
        String derived = regel("afgeleide sluitingsdag", "Kustwacht", "afgeleide sluitingsdag",
                own("sluitingsdag", "Kustwacht"), null);
        String ir = document(outcomeIf(comparison("later", own("vinddag"), months("plus",
                viaKustwacht("afgeleide sluitingsdag"), "3", "de afgeleide sluitingsdag … plus 3 mnd"))), derived);
        assertNull(play(ir, caseDocument(List.of(kustwacht("K", "2026-06-01"), haven("H", "2026-01-01", "K"),
                        haven("H3", "2026-02-01", null), jutter("J", "2026-03-01", "H"), jutter("J4", null, "H3"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2026-05-15", "J"), vondst("U", "2026-09-15", "J4")),
                date("V", "2026-09-15") + ", " + empty("W") + ", " + empty("U"))), "drie rollen");
    }

    @Test void a_date_arithmetic_with_a_parameter_two_far_roles() throws Exception {
        // "… indien zijn vinddag later is dan de afgeleide openingsdag van de haven van de jutter van de Vondst plus de
        // wachttijd", wachttijd 6 mnd: the comparison binds the parameter as a pattern variable. ref and clingo: V
        // 15-09-2026 (later than 01-07-2026), W leeg.
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + viaHaven("afgeleide openingsdag")
                + ",\"right\":{\"node\":\"ParameterReference\",\"parameter\":\"wachttijd\",\"source\":{\"snippet\":\"de wachttijd\"}},"
                + "\"type\":" + DATE + ",\"source\":{\"snippet\":\"de afgeleide openingsdag … plus de wachttijd\"}}";
        String ir = document(outcomeIf(comparison("later", own("vinddag"), plus)), DERIVED_OPENINGSDAG);
        String caseDocument = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2026-05-15", "J")))
                + "],\"parameters\":{\"wachttijd\":{\"number\":\"6\"}}},\"expected\":["
                + date("V", "2026-09-15") + ", " + empty("W") + "]}";
        assertNull(play(ir, caseDocument), "plus de wachttijd");
    }

    @Test void the_division_by_empty_under_a_condition_two_far_roles() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als zijn waarde gedeeld door zijn deler indien de
        // afgeleide openingsdag van de haven van de jutter van de Vondst gevuld is", the derivation after the reader.
        // The §6.5 companion repeats the host's voorwaarde, so it must see the derivation too. ref and clingo: V (no
        // deler) leeg and inconsistent; with deler 5 uitkomstwaarde 2,00 and consistent.
        String division = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + own("waarde") + ",\"right\":" + own("deler")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"zijn waarde gedeeld door zijn deler\"}}";
        String ir = document(regel("uitkomstwaarde", "Vondst", "uitkomstwaarde", division,
                predicate("filled", viaHaven("afgeleide openingsdag"))), DERIVED_OPENINGSDAG);
        assertNull(play(ir, caseDocument(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        instance("V", "Vondst", "\"waarde\":{\"number\":\"10\"}", "\"jutter\":[\"J\"]")),
                "{\"kind\":\"attributeEmpty\",\"label\":\"V\",\"attribute\":\"uitkomstwaarde\"}, {\"kind\":\"inconsistent\"}")),
                "de deling door een lege deler");
        assertNull(play(ir, caseDocument(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        instance("V", "Vondst", "\"waarde\":{\"number\":\"10\"}, \"deler\":{\"number\":\"5\"}", "\"jutter\":[\"J\"]")),
                "{\"kind\":\"attributeValue\",\"label\":\"V\",\"attribute\":\"uitkomstwaarde\",\"value\":{\"number\":\"2.00\"}},"
                        + " {\"kind\":\"consistent\"}")),
                "de deling door 5");
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWO_DECIMALS = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String MND = "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";

    private static String attribute(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objectType(String naam, String plural, boolean animate, String... attributes) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + animate + ",\"plural\":\"" + plural
                + "\",\"attributes\":[" + String.join(",", attributes) + "],\"characteristics\":[]}";
    }

    private static String factType(String naam, String oneRole, String oneType, String manyRole, String manyPlural, String manyType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + oneRole + "\",\"objectType\":\"" + oneType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + manyRole + "\",\"objectType\":\"" + manyType + "\",\"plural\":\"" + manyPlural
                + "\",\"many\":true}]}";
    }

    /** Kustwacht, Haven, Jutter and Vondst; a Vondst has one jutter, a Jutter one haven and one werver, a Haven one
     *  kustwacht. */
    private static final String MODEL = "{\"objectTypes\":["
            + objectType("Kustwacht", "Kustwachten", false, attribute("sluitingsdag", DATE), attribute("afgeleide sluitingsdag", DATE)) + ","
            + objectType("Haven", "Havens", false, attribute("openingsdag", DATE), attribute("afgeleide openingsdag", DATE)) + ","
            + objectType("Jutter", "Jutters", true, attribute("vergunningsdag", DATE)) + ","
            + objectType("Vondst", "Vondsten", false, attribute("vinddag", DATE), attribute("uitkomst", DATE),
                    attribute("waarde", WHOLE), attribute("deler", WHOLE), attribute("uitkomstwaarde", TWO_DECIMALS))
            + "],\"factTypes\":["
            + factType("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + factType("HavenHeeftJutter", "haven", "Haven", "havenjutter", "havenjutters", "Jutter") + ","
            + factType("KustwachtBewaaktHaven", "kustwacht", "Kustwacht", "bewaakte haven", "bewaakte havens", "Haven") + ","
            + factType("Werving", "werver", "Kustwacht", "geworven jutter", "geworven jutters", "Jutter")
            + "],\"parameters\":[{\"node\":\"Parameter\",\"name\":\"wachttijd\",\"datatype\":{\"node\":\"Number\",\"whole\":true,"
            + "\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + MND + "}}],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\","
            + "\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]},{\"node\":\"Unit\",\"abbreviation\":\"mnd\","
            + "\"system\":\"Tijd\",\"canonical\":\"mnd\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"maand\"]}]}],"
            + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String roleBinding(String factType, String role) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}";
    }

    private static String own(String attribute) { return own(attribute, "Vondst"); }

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribute + "\"}}";
    }

    /** A read of {@code attribute} on {@code type} over the role hops {@code path}, from the Vondst. */
    private static String navigated(String attribute, String type, String snippet, String... path) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", path) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribute) {
        return navigated(attribute, "Jutter", "de " + attribute + " van de jutter van de Vondst",
                roleBinding("JutterHeeftVondst", "jutter"));
    }

    /** "de &lt;attribuut&gt; van de haven van de jutter van de Vondst": two hops. */
    private static String viaHaven(String attribute) {
        return navigated(attribute, "Haven", "de " + attribute + " van de haven van de jutter van de Vondst",
                roleBinding("JutterHeeftVondst", "jutter"), roleBinding("HavenHeeftJutter", "haven"));
    }

    /** "de &lt;attribuut&gt; van de kustwacht van de haven van de jutter van de Vondst": three hops. */
    private static String viaKustwacht(String attribute) {
        return navigated(attribute, "Kustwacht", "de " + attribute + " van de kustwacht van de haven van de jutter van de Vondst",
                roleBinding("JutterHeeftVondst", "jutter"), roleBinding("HavenHeeftJutter", "haven"),
                roleBinding("KustwachtBewaaktHaven", "kustwacht"));
    }

    /** {@code date} plus or minus {@code n} months. */
    private static String months(String op, String date, String n, String snippet) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + date + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"" + n + "\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\","
                + "\"unit\":" + MND + "},\"source\":{\"snippet\":\"" + n + " mnd\"}},\"type\":" + DATE
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static String dateLiteral(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATE + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String comparison(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicate(String category, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + category + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + category + "\"}}";
    }

    /** One rule with one version; {@code condition} null for none. */
    private static String regel(String naam, String type, String target, String value, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + target + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + target + "\"}},\"value\":" + value
                + (condition == null ? "" : ",\"condition\":" + condition) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien &lt;voorwaarde&gt;." */
    private static String outcomeIf(String condition) {
        return regel("uitkomst", "Vondst", "uitkomst", own("vinddag"), condition);
    }

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven." */
    private static final String WERVING = "\"factCreations\":[{\"node\":\"FactCreation\",\"name\":\"werving\",\"versions\":[{"
            + "\"factType\":\"HavenHeeftJutter\",\"anchorRole\":\"haven\",\"targetRole\":\"havenjutter\",\"anchor\":\"Haven\","
            + "\"path\":[" + roleBinding("KustwachtBewaaktHaven", "kustwacht") + "," + roleBinding("Werving", "geworven jutter")
            + "],\"validity\":{\"node\":\"Validity\"}}]}],";

    /** H (kustwacht K) and J (werver K) are linked by no haven relation until the werving; J2's werver K2 guards no
     *  haven; V's jutter is J, X's J2. */
    private static final List<String> WERVING_SITUATION = List.of(kustwacht("K", "2026-06-01"), kustwacht("K2", null),
            haven("H", "2026-01-01", "K"), instance("J", "Jutter", "\"vergunningsdag\":{\"date\":\"2026-03-01\"}", "\"werver\":[\"K\"]"),
            instance("J2", "Jutter", "", "\"werver\":[\"K2\"]"), vondst("V", "2026-09-15", "J"), vondst("X", "2026-09-15", "J2"));

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String DERIVED_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            own("openingsdag", "Haven"), null);

    private static String document(String... rules) { return document(List.of(rules), ""); }

    /** {@code factCreations} is empty, or the whole member with its trailing comma. */
    private static String document(List<String> rules, String factCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + (factCreations.isEmpty() ? "\"factCreations\":[]," : factCreations)
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String dayAttribute(String attribute, String iso) {
        return iso == null ? "" : "\"" + attribute + "\":{\"date\":\"" + iso + "\"}";
    }

    private static String role(String role, String label) {
        return label == null ? "" : "\"" + role + "\":[\"" + label + "\"]";
    }

    private static String kustwacht(String label, String sluitingsdag) {
        return instance(label, "Kustwacht", dayAttribute("sluitingsdag", sluitingsdag), "");
    }

    private static String haven(String label, String openingsdag, String kustwacht) {
        return instance(label, "Haven", dayAttribute("openingsdag", openingsdag), role("kustwacht", kustwacht));
    }

    private static String jutter(String label, String vergunningsdag, String haven) {
        return instance(label, "Jutter", dayAttribute("vergunningsdag", vergunningsdag), role("haven", haven));
    }

    private static String vondst(String label, String vinddag, String jutter) {
        return instance(label, "Vondst", dayAttribute("vinddag", vinddag), role("jutter", jutter));
    }

    /** The uitkomst of {@code label} is {@code iso}. */
    private static String date(String label, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    /** The uitkomst of {@code label} is leeg. */
    private static String empty(String label) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\"}";
    }

    private static String caseDocument(List<String> instances, String expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{}},\"expected\":[" + expected + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, false);
    }

    private static String playDrl(String ir, String caseDocument) throws Exception {
        return play(ir, caseDocument, true);
    }

    private static String play(String ir, String caseDocument, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("keten-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(caseDocument);
        return CaseRunner.playCase(tmp, c, drl);
    }
}
