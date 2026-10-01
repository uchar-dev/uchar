package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A §6.5 division or a §6.6 root in a rule's value whose operand folds over a role: "(100 gedeeld door (de som van het
 * gewicht van alle vondsten van de Jutter)) rekenkundig afgerond op 2 decimalen", a minimum as the divisor, a sum as
 * the dividend, a fold inside a compound divisor, and the root of a sum minus 10. RUNTIME-SEMANTICS ch. 1.2: a filled
 * dividend over an empty divisor, and a negative radicand, degrade to leeg and deposit on the §9.5 channel (ch. 5.6);
 * an empty sum or minimum is leeg (ch. 1.2e), so over no vondsten the division deposits. Both Drools branches surface
 * the deposit through a companion of the rule; until this witness a companion read only an own or to-one navigated
 * attribute, so a fold operand got none. Every document here is authored in this class on Jutterswet material, each
 * one measured form with the values {@code ref} and clingo answer (clingo does not translate a rounded root; there
 * {@code ref} alone).
 *
 * <p>Situation (each case, unless a row says otherwise): J1 with V1 (vinddag 12-03-2026, gewicht 4), V2 (05-03-2026,
 * 7) and V3 (no values); J2 without vondsten; J3 with only V4 (no values); J4 with V5 (20-03-2026, 9). Every Jutter has
 * basis 2, and deler 2 except J1.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of main, both branches): every row whose case is
 * inconsistent answered {@code verwacht inconsistent, werkelijk consistent}, in the DRL-text branch also the row with
 * the fold under a Let, which the exec branch already refused. Two rows are flanks, green before and after: a filled
 * sum deposits nothing, and a count of 0 as the divisor is a fault in every executor, not a deposit.
 */
class VouwBegeleiderWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_deling_door_een_lege_som_meldt_de_fout() throws Exception {
        // J2 and J3 fold no gewicht: the sum is leeg, 100 over leeg is leeg and deposits. ref and clingo: 9,09, leeg,
        // leeg and 11,11, inconsistent.
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), SOM, ONBEGRENSD)), null));
        String zaak = zaak(situatie(), heffing("9.09", null, null, "11.11"), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void een_deling_door_een_leeg_minimum_meldt_de_fout() throws Exception {
        // The minimum of the gewicht as the divisor. ref and clingo: 25,00, leeg, leeg and 11,11, inconsistent.
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), MINIMUM, ONBEGRENSD)), null));
        String zaak = zaak(situatie(), heffing("25.00", null, null, "11.11"), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void een_som_gedeeld_door_een_lege_deler_meldt_de_fout() throws Exception {
        // "((de som …) gedeeld door zijn deler)": the fold is the dividend. J1 (sum 11) has no deler and deposits; J2
        // and J3 divide a leeg sum, which is 0 and deposits nothing. ref and clingo: leeg, 0,00, 0,00 and 4,50,
        // inconsistent.
        String ir = document(regel(afgerond(binair("dividedBy", SOM, eigen("deler"), ONBEGRENSD)), null));
        String zaak = zaak(situatie(), heffing(null, "0.00", "0.00", "4.50"), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void een_samengestelde_deler_met_een_vouw() throws Exception {
        // "(100 gedeeld door ((de som …) verminderd met 1))": leeg verminderd met 1 stays leeg (ch. 1.2), so the divisor
        // is leeg where the sum is. ref and clingo: 10,00, leeg, leeg and 12,50, inconsistent.
        String deler = binair("reducedBy", SOM, getal(1), GEHEEL);
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), deler, ONBEGRENSD)), null));
        String zaak = zaak(situatie(), heffing("10.00", null, null, "12.50"), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void de_wortel_van_een_negatieve_som_meldt_de_fout() throws Exception {
        // "de wortel van ((de som …) min 10) rekenkundig afgerond op 2 decimalen": J1 1, the others negative (leeg
        // min 10 is -10, ch. 1.2). ref: 1,00, leeg, leeg and leeg, inconsistent; clingo does not translate the rounded
        // root.
        String wortel = "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[" + binair("minus", SOM, getal(10), GEHEEL)
                + "],\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
                + "\"de wortel van ((de som …) min 10)\"}}";
        String ir = document(regel(afgerond(wortel), null));
        String zaak = zaak(situatie(), heffing("1.00", null, null, null), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void onder_de_voorwaarde_van_de_regel() throws Exception {
        // The division of the first row "indien zijn drempel gevuld is", drempel 1 for J1, J2 and J4: J2 deposits, J3
        // (no drempel) computes nothing. Without J2 the case is consistent: the companion carries the rule's condition.
        // ref and clingo: 9,09, leeg, leeg and 11,11, inconsistent; and 9,09, leeg and 11,11 for J1, J3 and J4,
        // consistent.
        String voorwaarde = "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + eigen("drempel")
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn drempel gevuld is\"}}";
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), SOM, ONBEGRENSD)), voorwaarde));
        List<String> met = new ArrayList<>();
        for (String i : situatie()) met.add(metDrempel(i, "J1", "J2", "J4"));
        String zaak = zaak(met, heffing("9.09", null, null, "11.11"), INCONSISTENT);
        assertNull(speel(ir, zaak), "exectak, J2 met drempel");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak, J2 met drempel");
        List<String> zonderJ2 = new ArrayList<>();
        for (String i : situatie()) if (!i.contains("\"J2\"")) zonderJ2.add(metDrempel(i, "J1", "J4"));
        String consistent = zaak(zonderJ2, verwacht("J1", "9.09") + "," + verwacht("J3", null) + "," + verwacht("J4", "11.11"),
                CONSISTENT);
        assertNull(speel(ir, consistent), "exectak, J3 zonder drempel");
        assertNull(speelDrl(ir, consistent), "DRL-teksttak, J3 zonder drempel");
    }

    @Test void een_vouw_onder_een_let_naast_een_deling_weigert_in_beide_takken() throws Exception {
        // "(de vangst gedeeld door zijn deler) rekenkundig afgerond op 2 decimalen. Daarbij geldt: de vangst is de som
        // …": J1's vangst is 11 over no deler and deposits; a leeg vangst ends the Let before the division (ch. 10.6), so
        // J2 and J3 deposit nothing. ref and clingo: leeg, leeg, leeg and 4,50, inconsistent. A companion re-reads the
        // site's need and the need is a fold, which neither branch can read there; both refuse the rule by name.
        String let = "{\"node\":\"Let\",\"type\":" + TWEE_DECIMALEN + ",\"binder\":\"vangst\",\"value\":" + SOM + ",\"body\":"
                + afgerond(binair("dividedBy", "{\"node\":\"BinderRef\",\"name\":\"vangst\",\"source\":{\"snippet\":\"de vangst\"}}",
                        eigen("deler"), ONBEGRENSD)) + ",\"source\":{\"snippet\":\"de vangst is de som …\"}}";
        String ir = document(regel(let, null));
        String zaak = zaak(situatie(), heffing(null, null, null, "4.50"), INCONSISTENT);
        String zin = "een vouw over een setbron en een deling of wortel onder één Let of IfThenElse";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> speel(ir, zaak));
        assertTrue(e.getMessage().contains(zin), e.getMessage());
        CaseRunner.Excluded d = assertThrows(CaseRunner.Excluded.class, () -> speelDrl(ir, zaak));
        assertTrue(d.getMessage().contains(zin), d.getMessage());
    }

    // ---------------- the flanks ----------------

    @Test void een_gevulde_som_meldt_niets() throws Exception {
        // The first row over J1 and J4 alone: every sum is filled. ref and clingo: 9,09 and 11,11, consistent.
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), SOM, ONBEGRENSD)), null));
        List<String> alleen = new ArrayList<>();
        for (String i : situatie()) if (!i.contains("\"J2\"") && !i.contains("\"J3\"")) alleen.add(i);
        String zaak = zaak(alleen, verwacht("J1", "9.09") + "," + verwacht("J4", "11.11"), CONSISTENT);
        assertNull(speel(ir, zaak), "exectak");
        assertNull(speelDrl(ir, zaak), "DRL-teksttak");
    }

    @Test void een_deler_die_nul_is_faalt() throws Exception {
        // "(100 gedeeld door het aantal vondsten van de Jutter)": J2 counts 0, a division by numeric zero, which is no
        // §9.5 deposit but a fault: ref "division by numeric zero", clingo the same guard, and both Drools branches stop
        // at fire.
        String ir = document(regel(afgerond(binair("dividedBy", getal(100), TELLING, ONBEGRENSD)), null));
        String zaak = zaak(situatie(), heffing("33.33", null, "100.00", "100.00"), CONSISTENT);
        assertThrows(RuntimeException.class, () -> speel(ir, zaak), "exectak");
        assertThrows(RuntimeException.class, () -> speelDrl(ir, zaak), "DRL-teksttak");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    /** A number that is not whole with 0 decimals: the unbounded form of IR-SPEC ch. 2.4 (c), as the frontend types a
     *  division. */
    private static final String ONBEGRENSD = "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    /** Jutter and Vondst; one jutter has many vondsten. */
    private static final String MODEL = "{\"objectTypes\":["
            + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
            + attribuut("heffing", TWEE_DECIMALEN) + "," + attribuut("basis", GEHEEL) + "," + attribuut("deler", GEHEEL) + ","
            + attribuut("drempel", GEHEEL) + "],\"characteristics\":[]},"
            + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
            + attribuut("vinddag", DATUM) + "," + attribuut("gewicht", GEHEEL) + "],\"characteristics\":[]}"
            + "],\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
            + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},"
            + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],"
            + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String rolbinding() {
        return "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Jutter\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String vouw(String soort, String snippet) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"gewicht\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"het gewicht van alle vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding() + ",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\""
                + snippet + "\"}}";
    }

    private static final String SOM = vouw("sum", "de som van het gewicht van alle vondsten van de Jutter");
    private static final String MINIMUM = vouw("minimum", "de minimale waarde van het gewicht van alle vondsten van de Jutter");
    /** "het aantal vondsten van de Jutter", as the frontend writes it. */
    private static final String TELLING = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":"
            + "\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van"
            + " de Jutter\"}}],\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding()
            + ",\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":{\"snippet\":"
            + "\"het aantal vondsten van de Jutter\"}}";

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + type
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** &lt;basis&gt; rekenkundig afgerond op 2 decimalen. */
    private static String afgerond(String basis) {
        return "{\"node\":\"Rounding\",\"base\":" + basis + ",\"mode\":\"halfAwayFromZero\",\"decimals\":2,\"type\":"
                + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"… rekenkundig afgerond op 2 decimalen\"}}";
    }

    /** "De heffing van een Jutter moet berekend worden als &lt;waarde&gt; [indien &lt;voorwaarde&gt;]." */
    private static String regel(String waarde, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"heffing\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"heffing\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":"
                + "\"De heffing van een Jutter\"}},\"value\":" + waarde + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
    }

    private static String document(String regel) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + regel + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String jutter(String label, boolean deler) {
        return instantie(label, "Jutter", "\"basis\":{\"number\":\"2\"}" + (deler ? ",\"deler\":{\"number\":\"2\"}" : ""), "");
    }

    private static String vondst(String label, String vinddag, String gewicht, String jutter) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":{\"date\":\"" + vinddag + "\"}");
        if (gewicht != null) a.add("\"gewicht\":{\"number\":\"" + gewicht + "\"}");
        return instantie(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment. */
    private static List<String> situatie() {
        return List.of(jutter("J1", false), jutter("J2", true), jutter("J3", true), jutter("J4", true),
                vondst("V1", "2026-03-12", "4", "J1"), vondst("V2", "2026-03-05", "7", "J1"), vondst("V3", null, null, "J1"),
                vondst("V4", null, null, "J3"), vondst("V5", "2026-03-20", "9", "J4"));
    }

    /** The Jutter instance {@code i} with drempel 1 where its label is one of {@code labels}. */
    private static String metDrempel(String i, String... labels) {
        for (String l : labels)
            if (i.startsWith("{\"label\":\"" + l + "\",\"objectType\":\"Jutter\""))
                return i.replace("\"attributes\":{", "\"attributes\":{\"drempel\":{\"number\":\"1\"},");
        return i;
    }

    private static final String INCONSISTENT = "{\"kind\":\"inconsistent\"}";
    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String verwacht(String label, String heffing) {
        return heffing == null
                ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"heffing\"}"
                : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"heffing\",\"value\":{\"number\":\""
                        + heffing + "\"}}";
    }

    /** The heffing of J1 to J4, a null value leeg. */
    private static String heffing(String... waarden) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < waarden.length; i++) uit.add(verwacht("J" + (i + 1), waarden[i]));
        return String.join(",", uit);
    }

    private static String zaak(List<String> instanties, String... verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    private static String speelDrl(String ir, String zaak) throws Exception {
        return speel(ir, zaak, true);
    }

    private static String speel(String ir, String zaak, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("vouw-begeleider", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, drl);
    }
}
