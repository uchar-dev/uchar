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
 * since it got the far-side join (GenavigeerdeVoorwaardeWitnessTest). Every document here is authored in this class
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
class KetenVoorwaardeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the card's reproductions ----------------

    @Test void r1_een_vergelijking_twee_rollen_ver_ziet_de_afgeleide_datum() throws Exception {
        // The card's R1: "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien zijn vinddag later is
        // dan de afgeleide openingsdag van de haven van de jutter van de Vondst", with "De afgeleide openingsdag van een
        // Haven moet gesteld worden op zijn openingsdag" before it, and once more after it. ref and clingo: V
        // 15-09-2026, W leeg.
        String uitkomst = uitkomstIndien(vergelijking("later", eigen("vinddag"), viaHaven("afgeleide openingsdag")));
        String zaak = zaak(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                datum("V", "2026-09-15") + ", " + leeg("W"));
        assertNull(speel(document(AFGELEIDE_OPENINGSDAG, uitkomst), zaak), "de afleiding voor de lezer");
        assertNull(speel(document(uitkomst, AFGELEIDE_OPENINGSDAG), zaak), "de afleiding na de lezer");
        // the DRL-text branch renders the same analysis since card drl-teksttak-op-truth-maintenance, and plays both
        // orders; until then it refused the comparison ("TODO voorwaarde")
        assertNull(speelDrl(document(AFGELEIDE_OPENINGSDAG, uitkomst), zaak), "drl-tekst: de afleiding voor de lezer");
        assertNull(speelDrl(document(uitkomst, AFGELEIDE_OPENINGSDAG), zaak), "drl-tekst: de afleiding na de lezer");
    }

    @Test void r2_gevuld_twee_rollen_ver_op_een_invoerwaarde() throws Exception {
        // The card's R2: "… indien de openingsdag van de haven van de jutter van de Vondst gevuld is". ref and clingo:
        // V 15-09-2026, W 15-12-2025.
        String ir = document(AFGELEIDE_OPENINGSDAG, uitkomstIndien(predicaat("filled", viaHaven("openingsdag"))));
        assertNull(speel(ir, zaak(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                datum("V", "2026-09-15") + ", " + datum("W", "2025-12-15"))), "gevuld over twee rollen");
    }

    @Test void r3_een_datumrekensom_aan_de_eigen_kant_een_rol_ver() throws Exception {
        // The card's R3: "… indien zijn vinddag min 6 mnd later is dan de vergunningsdag van de jutter van de Vondst".
        // ref and clingo: V 15-09-2026 (15-03-2026 is later than 01-03-2026), W leeg (15-06-2025 is not).
        String ir = document(AFGELEIDE_OPENINGSDAG, uitkomstIndien(vergelijking("later",
                maanden("minus", eigen("vinddag"), "6", "zijn vinddag min 6 mnd"), viaJutter("vergunningsdag"))));
        assertNull(speel(ir, zaak(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2025-12-15", "J")),
                datum("V", "2026-09-15") + ", " + leeg("W"))), "een datumrekensom een rol ver");
    }

    // ---------------- the flank and the join per hop ----------------

    @Test void zonder_relatie_op_de_tweede_hop_leest_de_keten_leeg() throws Exception {
        // V2's jutter has no haven, V3 has no jutter: RUNTIME-SEMANTICS ch. 1.2, a failed to-one navigation is leeg.
        // "… op 01-01-2026 indien de afgeleide openingsdag van de haven van de jutter van de Vondst leeg is", the
        // derivation first (with the reader first the test fires on the value before its derivation, the recorded
        // bound of the leeg test); and R1 over the same Vondsten. ref and clingo: the leeg test V1 leeg, V2 and V3
        // 01-01-2026; R1 V1 15-09-2026, V2 and V3 leeg.
        List<String> situatie = List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                jutter("J2", "2026-03-01", null), vondst("V1", "2026-09-15", "J"), vondst("V2", "2026-09-15", "J2"),
                vondst("V3", "2026-09-15", null));
        String leegtoets = regel("uitkomst", "Vondst", "uitkomst", datumLiteraal("2026-01-01"),
                predicaat("empty", viaHaven("afgeleide openingsdag")));
        assertNull(speel(document(AFGELEIDE_OPENINGSDAG, leegtoets), zaak(situatie,
                leeg("V1") + ", " + datum("V2", "2026-01-01") + ", " + datum("V3", "2026-01-01"))), "de leeg-toets");
        String r1 = uitkomstIndien(vergelijking("later", eigen("vinddag"), viaHaven("afgeleide openingsdag")));
        assertNull(speel(document(AFGELEIDE_OPENINGSDAG, r1), zaak(situatie,
                datum("V1", "2026-09-15") + ", " + leeg("V2") + ", " + leeg("V3"))), "de vergelijking");
    }

    @Test void de_tweede_relatie_die_een_feitcreatie_later_legt() throws Exception {
        // R2's test, with the haven of J not given but created: "Een havenjutter van een Haven is een geworven jutter
        // van de kustwacht van de Haven". The fact creation sets J's haven and updates J after V matched; only a join
        // on the Jutter runs the test again. ref and clingo: V 15-09-2026, X (its jutter has no werver) leeg.
        String ir = document(List.of(uitkomstIndien(predicaat("filled", viaHaven("openingsdag"))), AFGELEIDE_OPENINGSDAG),
                WERVING);
        assertNull(speel(ir, zaak(WERVING_SITUATIE, datum("V", "2026-09-15") + ", " + leeg("X"))), "de haven komt uit de werving");
    }

    @Test void de_waarde_over_de_keten_ziet_de_tweede_relatie_die_een_feitcreatie_later_legt() throws Exception {
        // The same fact creation under a rule whose VALUE reads the chain: "De uitkomst van een Vondst moet gesteld
        // worden op de openingsdag van de haven van de jutter van de Vondst". The value's far-side join on the reached
        // Haven alone does not see J's haven being set. ref and clingo: V 01-01-2026, X leeg. Measured 2026-10-01:
        // red on the transpiler of the fix of the voorwaarde (attribuut uitkomst: verwacht 2026-01-01, werkelijk leeg),
        // as on the claim.
        String ir = document(List.of(regel("uitkomst", "Vondst", "uitkomst", viaHaven("openingsdag"), null), AFGELEIDE_OPENINGSDAG),
                WERVING);
        assertNull(speel(ir, zaak(WERVING_SITUATIE, datum("V", "2026-01-01") + ", " + leeg("X"))), "de haven komt uit de werving");
    }

    // ---------------- further forms of (a) ----------------

    @Test void drie_rollen_ver_met_een_datumrekensom() throws Exception {
        // "… indien zijn vinddag later is dan de afgeleide sluitingsdag van de kustwacht van de haven van de jutter van
        // de Vondst plus 3 mnd", the derivation of the sluitingsdag after the reader. ref and clingo: V 15-09-2026
        // (later than 01-09-2026), W leeg (15-05-2026), U leeg (its haven has no kustwacht).
        String afgeleid = regel("afgeleide sluitingsdag", "Kustwacht", "afgeleide sluitingsdag",
                eigen("sluitingsdag", "Kustwacht"), null);
        String ir = document(uitkomstIndien(vergelijking("later", eigen("vinddag"), maanden("plus",
                viaKustwacht("afgeleide sluitingsdag"), "3", "de afgeleide sluitingsdag … plus 3 mnd"))), afgeleid);
        assertNull(speel(ir, zaak(List.of(kustwacht("K", "2026-06-01"), haven("H", "2026-01-01", "K"),
                        haven("H3", "2026-02-01", null), jutter("J", "2026-03-01", "H"), jutter("J4", null, "H3"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2026-05-15", "J"), vondst("U", "2026-09-15", "J4")),
                datum("V", "2026-09-15") + ", " + leeg("W") + ", " + leeg("U"))), "drie rollen");
    }

    @Test void een_datumrekensom_met_een_parameter_twee_rollen_ver() throws Exception {
        // "… indien zijn vinddag later is dan de afgeleide openingsdag van de haven van de jutter van de Vondst plus de
        // wachttijd", wachttijd 6 mnd: the comparison binds the parameter as a pattern variable. ref and clingo: V
        // 15-09-2026 (later than 01-07-2026), W leeg.
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + viaHaven("afgeleide openingsdag")
                + ",\"right\":{\"node\":\"ParameterReference\",\"parameter\":\"wachttijd\",\"source\":{\"snippet\":\"de wachttijd\"}},"
                + "\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de afgeleide openingsdag … plus de wachttijd\"}}";
        String ir = document(uitkomstIndien(vergelijking("later", eigen("vinddag"), plus)), AFGELEIDE_OPENINGSDAG);
        String zaak = "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":["
                + String.join(",", List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        vondst("V", "2026-09-15", "J"), vondst("W", "2026-05-15", "J")))
                + "],\"parameters\":{\"wachttijd\":{\"number\":\"6\"}}},\"expected\":["
                + datum("V", "2026-09-15") + ", " + leeg("W") + "]}";
        assertNull(speel(ir, zaak), "plus de wachttijd");
    }

    @Test void de_deling_door_leeg_onder_een_voorwaarde_twee_rollen_ver() throws Exception {
        // "De uitkomstwaarde van een Vondst moet berekend worden als zijn waarde gedeeld door zijn deler indien de
        // afgeleide openingsdag van de haven van de jutter van de Vondst gevuld is", the derivation after the reader.
        // The §6.5 companion repeats the host's voorwaarde, so it must see the derivation too. ref and clingo: V (no
        // deler) leeg and inconsistent; with deler 5 uitkomstwaarde 2,00 and consistent.
        String deling = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + eigen("waarde") + ",\"right\":" + eigen("deler")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"zijn waarde gedeeld door zijn deler\"}}";
        String ir = document(regel("uitkomstwaarde", "Vondst", "uitkomstwaarde", deling,
                predicaat("filled", viaHaven("afgeleide openingsdag"))), AFGELEIDE_OPENINGSDAG);
        assertNull(speel(ir, zaak(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        instantie("V", "Vondst", "\"waarde\":{\"number\":\"10\"}", "\"jutter\":[\"J\"]")),
                "{\"kind\":\"attributeEmpty\",\"label\":\"V\",\"attribute\":\"uitkomstwaarde\"}, {\"kind\":\"inconsistent\"}")),
                "de deling door een lege deler");
        assertNull(speel(ir, zaak(List.of(haven("H", "2026-01-01", null), jutter("J", "2026-03-01", "H"),
                        instantie("V", "Vondst", "\"waarde\":{\"number\":\"10\"}, \"deler\":{\"number\":\"5\"}", "\"jutter\":[\"J\"]")),
                "{\"kind\":\"attributeValue\",\"label\":\"V\",\"attribute\":\"uitkomstwaarde\",\"value\":{\"number\":\"2.00\"}},"
                        + " {\"kind\":\"consistent\"}")),
                "de deling door 5");
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String MND = "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, String... attributen) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[]}";
    }

    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\",\"plural\":\"" + veelMeervoud
                + "\",\"many\":true}]}";
    }

    /** Kustwacht, Haven, Jutter and Vondst; a Vondst has one jutter, a Jutter one haven and one werver, a Haven one
     *  kustwacht. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Kustwacht", "Kustwachten", false, attribuut("sluitingsdag", DATUM), attribuut("afgeleide sluitingsdag", DATUM)) + ","
            + objecttype("Haven", "Havens", false, attribuut("openingsdag", DATUM), attribuut("afgeleide openingsdag", DATUM)) + ","
            + objecttype("Jutter", "Jutters", true, attribuut("vergunningsdag", DATUM)) + ","
            + objecttype("Vondst", "Vondsten", false, attribuut("vinddag", DATUM), attribuut("uitkomst", DATUM),
                    attribuut("waarde", GEHEEL), attribuut("deler", GEHEEL), attribuut("uitkomstwaarde", TWEE_DECIMALEN))
            + "],\"factTypes\":["
            + feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + feittype("HavenHeeftJutter", "haven", "Haven", "havenjutter", "havenjutters", "Jutter") + ","
            + feittype("KustwachtBewaaktHaven", "kustwacht", "Kustwacht", "bewaakte haven", "bewaakte havens", "Haven") + ","
            + feittype("Werving", "werver", "Kustwacht", "geworven jutter", "geworven jutters", "Jutter")
            + "],\"parameters\":[{\"node\":\"Parameter\",\"name\":\"wachttijd\",\"datatype\":{\"node\":\"Number\",\"whole\":true,"
            + "\"decimals\":0,\"sign\":\"unspecified\",\"unit\":" + MND + "}}],\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\","
            + "\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]},{\"node\":\"Unit\",\"abbreviation\":\"mnd\","
            + "\"system\":\"Tijd\",\"canonical\":\"mnd\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"maand\"]}]}],"
            + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String eigen(String attribuut) { return eigen(attribuut, "Vondst"); }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** A read of {@code attribuut} on {@code type} over the role hops {@code pad}, from the Vondst. */
    private static String genavigeerd(String attribuut, String type, String snippet, String... pad) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"" + snippet + "\"},\"path\":[" + String.join(",", pad) + "]}";
    }

    /** "de &lt;attribuut&gt; van de jutter van de Vondst": one hop. */
    private static String viaJutter(String attribuut) {
        return genavigeerd(attribuut, "Jutter", "de " + attribuut + " van de jutter van de Vondst",
                rolbinding("JutterHeeftVondst", "jutter"));
    }

    /** "de &lt;attribuut&gt; van de haven van de jutter van de Vondst": two hops. */
    private static String viaHaven(String attribuut) {
        return genavigeerd(attribuut, "Haven", "de " + attribuut + " van de haven van de jutter van de Vondst",
                rolbinding("JutterHeeftVondst", "jutter"), rolbinding("HavenHeeftJutter", "haven"));
    }

    /** "de &lt;attribuut&gt; van de kustwacht van de haven van de jutter van de Vondst": three hops. */
    private static String viaKustwacht(String attribuut) {
        return genavigeerd(attribuut, "Kustwacht", "de " + attribuut + " van de kustwacht van de haven van de jutter van de Vondst",
                rolbinding("JutterHeeftVondst", "jutter"), rolbinding("HavenHeeftJutter", "haven"),
                rolbinding("KustwachtBewaaktHaven", "kustwacht"));
    }

    /** {@code datum} plus or minus {@code n} months. */
    private static String maanden(String op, String datum, String n, String snippet) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + datum + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"" + n + "\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\","
                + "\"unit\":" + MND + "},\"source\":{\"snippet\":\"" + n + " mnd\"}},\"type\":" + DATUM
                + ",\"source\":{\"snippet\":\"" + snippet + "\"}}";
    }

    private static String datumLiteraal(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATUM + ",\"source\":{\"snippet\":\"" + iso + "\"}}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    /** One rule with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String type, String doel, String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"" + type + "\","
                + "\"source\":{\"snippet\":\"De " + doel + "\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn vinddag indien &lt;voorwaarde&gt;." */
    private static String uitkomstIndien(String voorwaarde) {
        return regel("uitkomst", "Vondst", "uitkomst", eigen("vinddag"), voorwaarde);
    }

    /** "Een havenjutter van een Haven is een geworven jutter van de kustwacht van de Haven." */
    private static final String WERVING = "\"factCreations\":[{\"node\":\"FactCreation\",\"name\":\"werving\",\"versions\":[{"
            + "\"factType\":\"HavenHeeftJutter\",\"anchorRole\":\"haven\",\"targetRole\":\"havenjutter\",\"anchor\":\"Haven\","
            + "\"path\":[" + rolbinding("KustwachtBewaaktHaven", "kustwacht") + "," + rolbinding("Werving", "geworven jutter")
            + "],\"validity\":{\"node\":\"Validity\"}}]}],";

    /** H (kustwacht K) and J (werver K) are linked by no haven relation until the werving; J2's werver K2 guards no
     *  haven; V's jutter is J, X's J2. */
    private static final List<String> WERVING_SITUATIE = List.of(kustwacht("K", "2026-06-01"), kustwacht("K2", null),
            haven("H", "2026-01-01", "K"), instantie("J", "Jutter", "\"vergunningsdag\":{\"date\":\"2026-03-01\"}", "\"werver\":[\"K\"]"),
            instantie("J2", "Jutter", "", "\"werver\":[\"K2\"]"), vondst("V", "2026-09-15", "J"), vondst("X", "2026-09-15", "J2"));

    /** "De afgeleide openingsdag van een Haven moet gesteld worden op zijn openingsdag." */
    private static final String AFGELEIDE_OPENINGSDAG = regel("afgeleide openingsdag", "Haven", "afgeleide openingsdag",
            eigen("openingsdag", "Haven"), null);

    private static String document(String... regels) { return document(List.of(regels), ""); }

    /** {@code factCreations} is empty, or the whole member with its trailing comma. */
    private static String document(List<String> regels, String factCreations) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + (factCreations.isEmpty() ? "\"factCreations\":[]," : factCreations)
                + "\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String dagAttribuut(String attribuut, String iso) {
        return iso == null ? "" : "\"" + attribuut + "\":{\"date\":\"" + iso + "\"}";
    }

    private static String rol(String rol, String label) {
        return label == null ? "" : "\"" + rol + "\":[\"" + label + "\"]";
    }

    private static String kustwacht(String label, String sluitingsdag) {
        return instantie(label, "Kustwacht", dagAttribuut("sluitingsdag", sluitingsdag), "");
    }

    private static String haven(String label, String openingsdag, String kustwacht) {
        return instantie(label, "Haven", dagAttribuut("openingsdag", openingsdag), rol("kustwacht", kustwacht));
    }

    private static String jutter(String label, String vergunningsdag, String haven) {
        return instantie(label, "Jutter", dagAttribuut("vergunningsdag", vergunningsdag), rol("haven", haven));
    }

    private static String vondst(String label, String vinddag, String jutter) {
        return instantie(label, "Vondst", dagAttribuut("vinddag", vinddag), rol("jutter", jutter));
    }

    /** The uitkomst of {@code label} is {@code iso}. */
    private static String datum(String label, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    /** The uitkomst of {@code label} is leeg. */
    private static String leeg(String label) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\"}";
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    private static String speelDrl(String ir, String zaak) throws Exception {
        return speel(ir, zaak, true);
    }

    private static String speel(String ir, String zaak, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("keten-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, drl);
    }
}
