package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A computing rule whose voorwaarde lowers wholly into separate rule items — a quantifier fold over a role, an
 * aggregation comparison, a role check whose link is stored on the other end — and whose value reads an attribute of
 * its own subject that another rule derives later. Such a voorwaarde puts no test on the subject pattern, and a pattern
 * without a constraint has an empty reactivity mask: the rule fired once, when the voorwaarde first held, with the
 * operand still empty, and an update of the subject did not reach it again. RUNTIME-SEMANTICS ch. 5.1 (freshness)
 * re-evaluates a rule whenever a value it reads changes, its value's inputs as well as its condition's operands;
 * {@code ref/} and the ASP backend answer that fixpoint. Every document here is authored in this class on Jutterswet
 * material, and each is one measured form of the card's form space, with the values {@code ref} and clingo answer
 * (the clingo backend does not translate the maximum of two scalar operands; there {@code ref} alone).
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the jar of the claim, exec-model branch): the reproduction
 * {@code attribuut sluitdag: verwacht 31, werkelijk leeg}; the plus form {@code verwacht 32, werkelijk 1}, the empty
 * operand counted as 0; the quantifiers ten minste één and geen {@code verwacht 32, werkelijk 1}; the operand from a
 * defaults group {@code verwacht 36, werkelijk 1}; the fold over a characteristic with the operand's writer in the same
 * band {@code verwacht 37, werkelijk 1}; the aggregation comparison whose own threshold is derived later {@code
 * verwacht 21, werkelijk leeg}; the role check stored on the other end {@code verwacht 32, werkelijk 1}. Two rows are
 * flanks, green before and after: the writer of the operand declared before the rule, and a fold that stays false.
 */
class KwantorvoorwaardeOperandWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_berekening_rekent_opnieuw_zodra_haar_operand_er_is() throws Exception {
        // The card's R1: "De sluitdag van een Berging moet berekend worden als de maximale waarde van zijn laatste
        // bergdag en zijn gemelde dag indien de bergdag van alle vondsten van de Berging gevuld is", declared before
        // "bergdag" (vinddag plus 1) and "laatste bergdag" (the maximum over the vondsten). ref: 31 and 31.
        String ir = document(BERGINGMODEL, regels(
                sluitdag(maximum(eigen("laatste bergdag"), eigen("gemelde dag")), alleBergdagenGevuld()),
                BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(ir, zaak(List.of(berging("B", ""), vondst("V", "B", "30")),
                        waarde("B", "laatste bergdag", "31") + ", " + waarde("B", "sluitdag", "31"))),
                "het maximum van twee eigen operanden onder de kwantor alle");
    }

    @Test void een_lege_operand_telt_niet_als_nul() throws Exception {
        // The card's R3: the value "zijn laatste bergdag plus 1". Evaluated before the operand is written, the empty
        // operand coalesces to 0 (ch. 1.2) and the rule answered 1; at the fixpoint it is 31 plus 1. ref and clingo: 32.
        String ir = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("laatste bergdag"), getal("1")), alleBergdagenGevuld()),
                BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(ir, zaak(List.of(berging("B", ""), vondst("V", "B", "30")),
                        waarde("B", "laatste bergdag", "31") + ", " + waarde("B", "sluitdag", "32"))),
                "de som met een operand die later wordt afgeleid");
    }

    @Test void ten_minste_een_en_geen_herzien_ook() throws Exception {
        // "indien de bergdag van ten minste één van de vondsten van de Berging gevuld is" (V2 has no vinddag), and
        // "indien de vinddag van geen van de vondsten van de Berging leeg is", which holds at insertion already.
        // ref and clingo: sluitdag 32 in both.
        String tenMinste = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("laatste bergdag"), getal("1")),
                        kwantorvouw("atLeast", 1, predicaat("filled", element("bergdag")))),
                BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(tenMinste, zaak(List.of(berging("B", ""), vondst("V1", "B", "30"), vondst("V2", "B", null)),
                        waarde("B", "sluitdag", "32"))),
                "ten minste één");
        String geen = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("laatste bergdag"), getal("1")),
                        kwantorvouw("none", -1, predicaat("empty", element("vinddag")))),
                BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(geen, zaak(List.of(berging("B", ""), vondst("V1", "B", "30"), vondst("V2", "B", "20")),
                        waarde("B", "sluitdag", "32"))),
                "geen");
    }

    @Test void een_defaultsgroep_schrijft_de_operand() throws Exception {
        // "De gemelde dag van een Berging moet berekend worden als zijn openingsdag plus 5", with "Uitzondering op
        // gemelde dag: late opening … als zijn openingsdag plus 10 indien zijn openingsdag groter is dan 20", and the
        // rule "De sluitdag … als zijn gemelde dag plus 1 indien de vinddag van alle vondsten van de Berging gevuld
        // is". A group is added to the model after the rules, so in either source order it writes after the rule
        // fired. ref and clingo: gemelde dag 35, sluitdag 36.
        String groep = "\"prioritizedDefaults\":[{\"node\":\"PrioritizedDefaults\",\"name\":\"gemelde dag\",\"subject\":"
                + "\"Berging\",\"attribute\":\"gemelde dag\",\"defaults\":[{\"node\":\"Default\",\"justification\":"
                + vergelijking("greater", eigen("openingsdag"), getal("20")) + ",\"consequence\":"
                + plus(eigen("openingsdag"), getal("10")) + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\","
                + "\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}},\"consequence\":"
                + plus(eigen("openingsdag"), getal("5")) + "}],\"validity\":{\"node\":\"Validity\"}}]";
        String ir = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("gemelde dag"), getal("1")),
                        kwantorvouw("all", -1, predicaat("filled", element("vinddag"))))), groep);
        assertNull(speel(ir, zaak(List.of(berging("B", getal("openingsdag", "25")), vondst("V", "B", "30")),
                        waarde("B", "gemelde dag", "35") + ", " + waarde("B", "sluitdag", "36"))),
                "de operand uit een defaultsgroep");
    }

    @Test void een_kwantor_over_een_kenmerk_met_de_schrijver_in_dezelfde_band() throws Exception {
        // "indien ten minste één van de vondsten van de Berging geborgen is", geborgen given by the case. The
        // stratification reads a fold's matrix as a negated read and sinks the rule to band -1, below every writer in
        // band 0; here the operand's writer, "De gemelde dag … als zijn laatste bergdag plus 5" under the same fold,
        // sits in that band too and is declared after the rule. ref and clingo: gemelde dag 36, sluitdag 37.
        String geborgen = kwantorvouw("atLeast", 1, "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"geborgen\","
                + "\"negated\":false,\"source\":{\"snippet\":\"geborgen is\"}}");
        String gemeldeDag = regel("gemelde dag", "gemelde dag", plus(eigen("laatste bergdag"), getal("5")), geborgen);
        String ir = document(BERGINGMODEL_GEBORGEN, regels(
                sluitdag(plus(eigen("gemelde dag"), getal("1")), geborgen), gemeldeDag, BERGDAG, LAATSTE_BERGDAG), "");
        String v1 = "{\"label\":\"V1\",\"objectType\":\"Vondst\",\"attributes\":{" + getal("vinddag", "30")
                + "},\"roles\":{\"berging\":[\"B\"]},\"characteristics\":[\"geborgen\"]}";
        assertNull(speel(ir, zaak(List.of(berging("B", ""), v1, vondst("V2", "B", null)),
                        waarde("B", "gemelde dag", "36") + ", " + waarde("B", "sluitdag", "37"))),
                "de schrijver van de operand in de band van de regel");
    }

    @Test void een_voorwaarde_die_haar_eigen_drempel_later_krijgt() throws Exception {
        // "De sluitdag van een Berging moet berekend worden als 21 indien de maximale waarde van de bergdag van alle
        // vondsten van de Berging groter is dan zijn gemelde dag", the gemelde dag derived later (openingsdag 20 plus
        // 5). The value reads nothing: the voorwaarde reads its own threshold in the accumulate's result test, and
        // before the threshold is written the comparison with an empty operand is onwaar. ref and clingo: 25, 21.
        String ir = document(BERGINGMODEL, regels(
                sluitdag(getal("21"), vergelijking("greater", bergdagMaximum(), eigen("gemelde dag"))),
                BERGDAG, regel("gemelde dag", "gemelde dag", plus(eigen("openingsdag"), getal("5")), null)), "");
        assertNull(speel(ir, zaak(List.of(berging("B", getal("openingsdag", "20")), vondst("V", "B", "30")),
                        waarde("B", "gemelde dag", "25") + ", " + waarde("B", "sluitdag", "21"))),
                "de eigen drempel van een aggregatievergelijking");
    }

    @Test void een_rolcheck_aan_de_overkant() throws Exception {
        // "indien hij een berging is": the link is stored on the Vondst, so the check is an exists-join, a separate
        // rule item as the fold is. ref and clingo: sluitdag 32.
        String ir = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("laatste bergdag"), getal("1")), "{\"node\":\"RoleMembership\",\"factType\":"
                        + "\"BergingBetreftVondst\",\"role\":\"berging\",\"negated\":false,\"source\":{\"snippet\":"
                        + "\"hij een berging is\"}}"),
                BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(ir, zaak(List.of(berging("B", ""), vondst("V", "B", "30")), waarde("B", "sluitdag", "32"))),
                "de rolcheck aan de overkant");
    }

    @Test void de_schrijver_voor_de_berekening_speelde_al() throws Exception {
        // The card's R2, a flank: the rule declared after the writers of its operands fires after them. ref: 31.
        String ir = document(BERGINGMODEL, regels(BERGDAG, LAATSTE_BERGDAG,
                sluitdag(maximum(eigen("laatste bergdag"), eigen("gemelde dag")), alleBergdagenGevuld())), "");
        assertNull(speel(ir, zaak(List.of(berging("B", ""), vondst("V", "B", "30")), waarde("B", "sluitdag", "31"))),
                "de schrijvers eerst in de bron");
    }

    @Test void een_kwantor_die_onwaar_blijft_vuurt_niet() throws Exception {
        // A flank: V2 has no vinddag, so its bergdag stays empty and "alle … gevuld" never holds; the rule must not
        // fire on the reactivity the fix adds. ref and clingo: laatste bergdag 31, sluitdag leeg.
        String ir = document(BERGINGMODEL, regels(
                sluitdag(plus(eigen("laatste bergdag"), getal("1")), alleBergdagenGevuld()), BERGDAG, LAATSTE_BERGDAG), "");
        assertNull(speel(ir, zaak(List.of(berging("B", ""), vondst("V1", "B", "30"), vondst("V2", "B", null)),
                        waarde("B", "laatste bergdag", "31") + ", " + leegVerwacht("B", "sluitdag"))),
                "de kwantor blijft onwaar");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String FEITTYPE = "{\"node\":\"FactType\",\"name\":\"BergingBetreftVondst\",\"reciprocal\":false,\"roles\":["
            + "{\"node\":\"Role\",\"name\":\"berging\",\"objectType\":\"Berging\",\"many\":false},"
            + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}";
    private static final String BRONROL = "{\"node\":\"RoleBinding\",\"factType\":\"BergingBetreftVondst\",\"role\":\"vondst\"}";

    /** Berging (laatste bergdag, gemelde dag, openingsdag, sluitdag), Vondst (vinddag, bergdag), one berging betreft
     *  meerdere vondsten. */
    private static final String BERGINGMODEL = model("");
    /** The same, with the characteristic geborgen on the Vondst. */
    private static final String BERGINGMODEL_GEBORGEN =
            model("{\"node\":\"Characteristic\",\"name\":\"geborgen\",\"kind\":\"adjectival\"}");

    /** "De bergdag van een Vondst moet berekend worden als zijn vinddag plus 1 indien zijn vinddag gevuld is." */
    private static final String BERGDAG = "{\"node\":\"Rule\",\"name\":\"bergdag\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
            + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"bergdag\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
            + "\"De bergdag van een Vondst\"}},\"value\":" + plus(attribuut("vinddag", "Vondst"), getal("1"))
            + ",\"condition\":" + predicaat("filled", attribuut("vinddag", "Vondst"))
            + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    /** "De laatste bergdag van een Berging moet berekend worden als de maximale waarde van de bergdag van alle vondsten
     *  van de Berging." */
    private static final String LAATSTE_BERGDAG = regel("laatste bergdag", "laatste bergdag", bergdagMaximum(), null);

    private static String model(String vondstKenmerk) {
        return "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Berging\",\"animate\":false,\"plural\":\"Bergingen\","
                + "\"attributes\":[" + veld("laatste bergdag") + "," + veld("gemelde dag") + "," + veld("openingsdag") + ","
                + veld("sluitdag") + "],\"characteristics\":[]},{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
                + "\"plural\":\"Vondsten\",\"attributes\":[" + veld("vinddag") + "," + veld("bergdag") + "],\"characteristics\":["
                + vondstKenmerk + "]}],\"factTypes\":[" + FEITTYPE + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
    }

    private static String veld(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}";
    }

    private static String attribuut(String naam, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + naam + "\",\"subjectType\":\"" + type
                + "\",\"source\":{\"snippet\":\"zijn " + naam + "\"}}";
    }

    private static String eigen(String naam) { return attribuut(naam, "Berging"); }

    private static String element(String naam) { return attribuut(naam, "Vondst"); }

    private static String getal(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String plus(String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"plus\"}}";
    }

    /** "de maximale waarde van a en b": the listed maximum of two scalar operands. */
    private static String maximum(String a, String b) {
        return "{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[" + a + "," + b + "],\"elementType\":\"Berging\","
                + "\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"de maximale waarde\"}}";
    }

    /** "de maximale waarde van de bergdag van alle vondsten van de Berging". */
    private static String bergdagMaximum() {
        return "{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[" + element("bergdag") + "],\"elementType\":\"Vondst\","
                + "\"sourceRole\":" + BRONROL + ",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"de maximale waarde van de bergdag\"}}";
    }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"" + soort + "\"}}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A quantifier fold over the vondsten of the Berging; {@code drempel} below 0 for a quantifier without one. */
    private static String kwantorvouw(String kwantor, int drempel, String toets) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + kwantor + "\"" + (drempel < 0 ? "" : ",\"threshold\":" + drempel)
                + ",\"elementType\":\"Vondst\",\"test\":" + toets + ",\"source\":{\"snippet\":\"" + kwantor + " van de vondsten\"},"
                + "\"sourceRole\":" + BRONROL + "}";
    }

    /** "de bergdag van alle vondsten van de Berging gevuld is". */
    private static String alleBergdagenGevuld() {
        return kwantorvouw("all", -1, predicaat("filled", element("bergdag")));
    }

    /** One rule on the Berging with one version; {@code voorwaarde} null for none. */
    private static String regel(String naam, String doel, String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"" + doel + "\",\"subjectType\":\"Berging\","
                + "\"source\":{\"snippet\":\"De " + doel + " van een Berging\"}},\"value\":" + waarde
                + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde) + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    }

    private static String sluitdag(String waarde, String voorwaarde) { return regel("sluitdag", "sluitdag", waarde, voorwaarde); }

    private static String regels(String... regels) { return String.join(",", regels); }

    /** {@code defaults} is empty, or the {@code prioritizedDefaults} member without a trailing comma. */
    private static String document(String model, String regels, String defaults) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + regels + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]"
                + (defaults.isEmpty() ? "" : "," + defaults) + "},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String berging(String label, String attributen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Berging\",\"attributes\":{" + attributen
                + "},\"roles\":{},\"characteristics\":[]}";
    }

    /** A Vondst of berging {@code berging}, with vinddag {@code vinddag} or none when it is null. */
    private static String vondst(String label, String berging, String vinddag) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{"
                + (vinddag == null ? "" : getal("vinddag", vinddag)) + "},\"roles\":{\"berging\":[\"" + berging
                + "\"]},\"characteristics\":[]}";
    }

    private static String getal(String attribuut, String waarde) {
        return "\"" + attribuut + "\":{\"number\":\"" + waarde + "\"}";
    }

    private static String waarde(String label, String attribuut, String getal) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"number\":\"" + getal + "\"}}";
    }

    private static String leegVerwacht(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("kwantorvoorwaarde-operand", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, false);
    }
}
