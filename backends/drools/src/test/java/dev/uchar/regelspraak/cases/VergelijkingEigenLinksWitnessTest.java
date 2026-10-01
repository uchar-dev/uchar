package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A numeric comparison with the subject's own operand on the left and a navigated one on the right: "Een Vondst is
 * zwaar indien zijn gewicht groter is dan de afgeleide diepte van de haven van de jutter van de Vondst". The mirrored
 * sentence ("de afgeleide diepte van de haven van de jutter van de Vondst kleiner is dan zijn gewicht") and "zijn
 * gewicht maal 1 groter is dan …" mean the same and lowered; this one did not. Every document here is authored in this
 * class on Jutterswet material, with the values {@code ref} and clingo answer; the far value is derived by a rule, so
 * the comparison also needs the far-side join a navigated condition takes.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of {@code 2a0c6d60}, exec-model branch): every
 * form with the own operand on the left was refused ({@code regel (voorwaarde buiten Phase-B scope)}), in a
 * characteristic assignment and in an ordinary rule, over one role and over two, under groter, groter of gelijk and
 * ongelijk; in a subselection the fold was refused. The mirrored form and the product played before and play after.
 */
class VergelijkingEigenLinksWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_kenmerk_twee_rollen_ver() throws Exception {
        // "Een Vondst is zwaar indien zijn gewicht groter is dan de afgeleide diepte van de haven van de jutter van de
        // Vondst". ref and clingo: V1 (12 against 10) zwaar; V2 (9), V3 (haven without diepte), V4 (jutter without
        // haven) not.
        assertNull(speel(document(zwaarIndien(vergelijking("greater", eigen("gewicht"), dieptevanDeHaven()))),
                zaak(kenmerk("V1", true) + "," + kenmerk("V2", false) + "," + kenmerk("V3", false) + "," + kenmerk("V4", false))),
                "groter, twee rollen ver");
    }

    @Test void een_kenmerk_onder_groter_of_gelijk_en_ongelijk() throws Exception {
        // "… groter of gelijk is aan …" and "… ongelijk is aan …": a leeg far value is ongelijk to a gewicht (RUNTIME-
        // SEMANTICS ch. 1.2). ref and clingo: V1 alone, and all four.
        assertNull(speel(document(zwaarIndien(vergelijking("greaterOrEqual", eigen("gewicht"), dieptevanDeHaven()))),
                zaak(kenmerk("V1", true) + "," + kenmerk("V2", false) + "," + kenmerk("V3", false) + "," + kenmerk("V4", false))),
                "groter of gelijk");
        assertNull(speel(document(zwaarIndien(vergelijking("notEqual", eigen("gewicht"), dieptevanDeHaven()))),
                zaak(kenmerk("V1", true) + "," + kenmerk("V2", true) + "," + kenmerk("V3", true) + "," + kenmerk("V4", true))),
                "ongelijk");
    }

    @Test void een_kenmerk_een_rol_ver() throws Exception {
        // "… indien zijn gewicht groter is dan de afgeleide drempel van de jutter van de Vondst". ref and clingo: V1 (12
        // against 8) and V2 (9) zwaar, V3 and V4 (jutters without a drempel) not.
        assertNull(speel(document(zwaarIndien(vergelijking("greater", eigen("gewicht"), drempelVanDeJutter()))),
                zaak(kenmerk("V1", true) + "," + kenmerk("V2", true) + "," + kenmerk("V3", false) + "," + kenmerk("V4", false))),
                "groter, één rol ver");
    }

    @Test void een_gewone_regel() throws Exception {
        // "De uitkomst van een Vondst moet gesteld worden op zijn gewicht indien zijn gewicht groter is dan …", two roles
        // and one role away. ref and clingo: V1 12 and the others leeg; V1 12, V2 9, V3 and V4 leeg.
        assertNull(speel(document(uitkomstIndien(vergelijking("greater", eigen("gewicht"), dieptevanDeHaven()))),
                zaak(getal("V1", 12) + "," + leeg("V2") + "," + leeg("V3") + "," + leeg("V4"))), "twee rollen ver");
        assertNull(speel(document(uitkomstIndien(vergelijking("greater", eigen("gewicht"), drempelVanDeJutter()))),
                zaak(getal("V1", 12) + "," + getal("V2", 9) + "," + leeg("V3") + "," + leeg("V4"))), "één rol ver");
    }

    @Test void in_een_subselectie() throws Exception {
        // "Het aantal zware vondsten van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn gewicht is groter dan de afgeleide drempel van de jutter van de Vondst". ref
        // and clingo: J1 2, J2 and J3 0.
        String telling = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\",\"filter\":{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":["
                + vergelijking("greater", eigen("gewicht"), drempelVanDeJutter()) + "],\"source\":{\"snippet\":\"aan de volgende"
                + " voorwaarde voldoen\"}},\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst") + ",\"type\":{\"node\":"
                + "\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":\"het aantal vondsten"
                + " van de Jutter die …\"}}";
        String regel = "{\"node\":\"Rule\",\"name\":\"aantal zware vondsten\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"aantal zware vondsten\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Het aantal zware vondsten van een Jutter\"}},\"value\":" + telling
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        assertNull(speel(document(regel), zaak(teller("J1", 2) + "," + teller("J2", 0) + "," + teller("J3", 0))),
                "in een subselectie");
    }

    @Test void de_gespiegelde_vorm_en_het_product_speelden_al() throws Exception {
        // "… de afgeleide diepte van de haven van de jutter van de Vondst kleiner is dan zijn gewicht" and "… zijn gewicht
        // maal 1 groter is dan …". ref and clingo: V1 alone.
        String verwacht = kenmerk("V1", true) + "," + kenmerk("V2", false) + "," + kenmerk("V3", false) + "," + kenmerk("V4", false);
        assertNull(speel(document(zwaarIndien(vergelijking("less", dieptevanDeHaven(), eigen("gewicht")))), zaak(verwacht)),
                "gespiegeld");
        String product = "{\"node\":\"Binary\",\"op\":\"times\",\"left\":" + eigen("gewicht") + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"number\":\"1\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"1\"}},\"type\":{\"node\":"
                + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"zijn gewicht maal 1\"}}";
        assertNull(speel(document(zwaarIndien(vergelijking("greater", product, dieptevanDeHaven()))), zaak(verwacht)), "maal 1");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}";
    }

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    /** Haven (diepte, afgeleide diepte), Jutter (drempel, afgeleide drempel, aantal zware vondsten) and Vondst (gewicht,
     *  uitkomst, kenmerk zwaar). A Vondst has one jutter, a Jutter one haven. */
    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Haven\",\"animate\":false,\"plural\":\"Havens\",\"attributes\":["
            + attribuut("diepte") + "," + attribuut("afgeleide diepte") + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
            + attribuut("drempel") + "," + attribuut("afgeleide drempel") + "," + attribuut("aantal zware vondsten")
            + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribuut("gewicht") + "," + attribuut("uitkomst") + "],\"characteristics\":[{\"node\":\"Characteristic\","
            + "\"name\":\"zwaar\",\"kind\":\"adjectival\"}]}],\"factTypes\":["
            + "{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
            + "\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":"
            + "\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]},"
            + "{\"node\":\"FactType\",\"name\":\"HavenHeeftJutter\",\"reciprocal\":false,\"roles\":[{\"node\":\"Role\","
            + "\"name\":\"haven\",\"objectType\":\"Haven\",\"many\":false},{\"node\":\"Role\",\"name\":\"havenjutter\",\"objectType\":"
            + "\"Jutter\",\"plural\":\"havenjutters\",\"many\":true}]}],"
            + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "de afgeleide diepte van de haven van de jutter van de Vondst": two roles. */
    private static String dieptevanDeHaven() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide diepte\",\"subjectType\":\"Haven\",\"source\":"
                + "{\"snippet\":\"de afgeleide diepte van de haven van de jutter van de Vondst\"},\"path\":["
                + rolbinding("JutterHeeftVondst", "jutter") + "," + rolbinding("HavenHeeftJutter", "haven") + "]}";
    }

    /** "de afgeleide drempel van de jutter van de Vondst": one role. */
    private static String drempelVanDeJutter() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide drempel\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"de afgeleide drempel van de jutter van de Vondst\"},\"path\":[" + rolbinding("JutterHeeftVondst", "jutter")
                + "]}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts
                + ",\"source\":{\"snippet\":\"vergelijking " + op + "\"}}";
    }

    /** "Een Vondst is zwaar indien &lt;voorwaarde&gt;." */
    private static String zwaarIndien(String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"zwaar\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"zwaar\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is zwaar\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde + ",\"initialisation\":false}]}";
    }

    /** "De uitkomst van een Vondst moet gesteld worden op zijn gewicht indien &lt;voorwaarde&gt;." */
    private static String uitkomstIndien(String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"uitkomst\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"uitkomst\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"De uitkomst van een Vondst\"}},\"value\":" + eigen("gewicht") + ",\"condition\":" + voorwaarde
                + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
    }

    /** The reader {@code lezer} before the two rules that derive the far values from the input. */
    private static String document(String lezer) {
        String diepte = "{\"node\":\"Rule\",\"name\":\"afgeleide diepte\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide diepte\",\"subjectType\":\"Haven\",\"source\":"
                + "{\"snippet\":\"De afgeleide diepte van een Haven\"}},\"value\":{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"diepte\",\"subjectType\":\"Haven\",\"source\":{\"snippet\":\"zijn diepte\"}},\"initialisation\":false,"
                + "\"presentation\":\"equation\"}]}";
        String drempel = "{\"node\":\"Rule\",\"name\":\"afgeleide drempel\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"afgeleide drempel\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"De afgeleide drempel van een Jutter\"}},\"value\":{\"node\":\"AttributeReference\","
                + "\"attribute\":\"drempel\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"zijn drempel\"}},"
                + "\"initialisation\":false,\"presentation\":\"equation\"}]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", List.of(lezer, diepte, drempel))
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    /** H1 (diepte 10) and H2 (none); J1 (drempel 8, haven H1), J2 (haven H2) and J3 (no haven); V1 (gewicht 12) and V2
     *  (9) of J1, V3 (12) of J2, V4 (12) of J3. */
    private static final List<String> SITUATIE = List.of(
            instantie("H1", "Haven", "\"diepte\":{\"number\":\"10\"}", ""), instantie("H2", "Haven", "", ""),
            instantie("J1", "Jutter", "\"drempel\":{\"number\":\"8\"}", "\"haven\":[\"H1\"]"),
            instantie("J2", "Jutter", "", "\"haven\":[\"H2\"]"), instantie("J3", "Jutter", "", ""),
            instantie("V1", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J1\"]"),
            instantie("V2", "Vondst", "\"gewicht\":{\"number\":\"9\"}", "\"jutter\":[\"J1\"]"),
            instantie("V3", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J2\"]"),
            instantie("V4", "Vondst", "\"gewicht\":{\"number\":\"12\"}", "\"jutter\":[\"J3\"]"));

    private static String kenmerk(String label, boolean aanwezig) {
        return "{\"kind\":\"" + (aanwezig ? "characteristicPresent" : "characteristicAbsent") + "\",\"label\":\"" + label
                + "\",\"characteristic\":\"zwaar\"}";
    }

    private static String getal(String label, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\",\"value\":{\"number\":\"" + n + "\"}}";
    }

    private static String leeg(String label) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"uitkomst\"}";
    }

    private static String teller(String label, int n) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"aantal zware vondsten\",\"value\":"
                + "{\"number\":\"" + n + "\"}}";
    }

    private static String zaak(String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", SITUATIE)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("vergelijking-eigen-links", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, false);
    }
}
