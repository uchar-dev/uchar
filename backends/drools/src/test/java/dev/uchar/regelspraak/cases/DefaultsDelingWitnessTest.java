package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A division, or a root, that is itself the consequence of a default: "Afwijking van verhouding: De verhouding van een
 * Vondst moet berekend worden als zijn gewogen gewicht gedeeld door zijn geschatte gewicht indien …" under the base
 * "De verhouding van een Vondst moet gesteld worden op 1,00". Over a leeg divisor (or a negative radicand) the
 * consequence is leeg, so the default falls through, and its deposit on the §9.5 channel does not persist
 * (RUNTIME-SEMANTICS ch. 7.1); a default that decides deposits nothing. Every document here is authored in this class on
 * Jutterswet material, with the values {@code ref} answers; clingo answers the same where it carries the form (the
 * division in a variant and in the base, the rounded division), refuses a rounded root by name, and for the leeg
 * divisor reports an inconsistency that ref and ch. 7.1 do not (recorded with the round that added this witness).
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of {@code 2a0c6d60}, exec-model branch): every
 * such group was refused, {@code kanaal-deponerende consequentie (deling) op defaults-target}, and the root in the same
 * words. A division under another operator ("(… gedeeld door …) plus 5,00") decides with the deposit and stays refused.
 *
 * <p><b>On the truth-maintenance branch every row is a refusal by name</b> (card tms-omschakeling, 2026-10-10). That
 * branch computes a default as soon as what it reads stands, a default a higher one displaces among them, so a division
 * by zero there is a fault on an intermediate state, which the branch withdraws once the higher default decides; whether
 * such a fault counts waits on the norm decision of card {@code 20261008-fout-op-tussenstand}. A root there gets no
 * §6.6 deposit from it, since a default deposits only where it decides (ch. 7.1). The destructive exec-model branch
 * carried the five forms above the last row; the loss is accepted, and each row keeps the values {@code ref} answers, so
 * that the card that carries the form turns the row back into a replay.
 */
class DefaultsDelingWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_deling_in_een_afwijking() throws Exception {
        // The division under "zijn gewogen gewicht is gevuld" and "zijn geschatte gewicht is groter dan 0" over the base
        // 1,00 ("Uitzondering op" lowers to the same group). ref and clingo: V1 (240 by 320) 0,75, V2 1,00.
        String groep = groep(standaard(alle(gevuld("gewogen gewicht"), groterDanNul("geschatte gewicht")), deling()),
                standaard(ALTIJD, EEN));
        geweigerd(groep, zaak(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null),
                verhouding("V1", "0.75") + "," + verhouding("V2", "1.00")), DELING);
    }

    @Test void een_deling_in_de_basisregel() throws Exception {
        // The division as the base, under "Afwijking van verhouding: niet gewogen" (1,00 where the gewogen gewicht is
        // leeg). ref and clingo: V1 0,75, V2 1,00.
        String groep = groep(standaard(leegToets("gewogen gewicht"), EEN),
                standaard(alle(gevuld("gewogen gewicht"), groterDanNul("geschatte gewicht")), deling()));
        geweigerd(groep, zaak(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null),
                verhouding("V1", "0.75") + "," + verhouding("V2", "1.00")), DELING);
    }

    @Test void een_deling_met_een_lege_deler() throws Exception {
        // The division under "zijn gewogen gewicht gevuld is" alone: V3 has no geschat gewicht, so its consequence is leeg,
        // the default falls through to 1,00, and nothing persists on the channel (ch. 7.1). ref: V1 0,75, V2 and V3 1,00,
        // consistent.
        String groep = groep(standaard(gevuld("gewogen gewicht"), deling()), standaard(ALTIJD, EEN));
        geweigerd(groep, zaak(vondst("V1", "320.00", "240.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "60.00"),
                verhouding("V1", "0.75") + "," + verhouding("V2", "1.00") + "," + verhouding("V3", "1.00") + "," + CONSISTENT),
                DELING);
    }

    @Test void een_afgeronde_deling() throws Exception {
        // "… gedeeld door zijn geschatte gewicht naar beneden afgerond op 2 decimalen". ref and clingo: V1 (200 by 300)
        // 0,66, V2 and V3 1,00.
        String groep = groep(standaard(gevuld("gewogen gewicht"), afgerond(deling())), standaard(ALTIJD, EEN));
        geweigerd(groep, zaak(vondst("V1", "300.00", "200.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "60.00"),
                verhouding("V1", "0.66") + "," + verhouding("V2", "1.00") + "," + verhouding("V3", "1.00")), DELING);
    }

    @Test void een_wortel() throws Exception {
        // "… de wortel van zijn gewogen gewicht naar beneden afgerond op 2 decimalen indien zijn gewogen gewicht gevuld
        // is": V3's radicand is negative, its consequence leeg, and the default falls through. ref: V1 (240) 15,49, V2
        // and V3 1,00, consistent; clingo refuses a rounded root by name.
        String wortel = "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[" + eigen("gewogen gewicht")
                + "],\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"nonNegative\"},\"source\":"
                + "{\"snippet\":\"de wortel van zijn gewogen gewicht\"}}";
        String groep = groep(standaard(gevuld("gewogen gewicht"), afgerond(wortel)), standaard(ALTIJD, EEN));
        geweigerd(groep, zaak(vondst("V1", null, "240.00") + "," + vondst("V2", "50.00", null) + ","
                        + vondst("V3", null, "-16.00"),
                verhouding("V1", "15.49") + "," + verhouding("V2", "1.00") + "," + verhouding("V3", "1.00") + "," + CONSISTENT),
                "een wortel in een default ('de wortel van zijn gewogen gewicht'), waarvan deze tak de §6.6-deponering "
                        + "niet maakt: een default deponeert alleen waar hij beslist (RUNTIME-SEMANTICS ch. 7.1)");
    }

    @Test void een_deling_onder_een_andere_bewerking_blijft_geweigerd() throws Exception {
        // "(zijn gewogen gewicht gedeeld door zijn geschatte gewicht) plus 5,00": over a leeg divisor the default decides
        // 5,00 and its deposit persists (ref and clingo report the inconsistency). The truth-maintenance branch refuses
        // it as every division in a default.
        String plus = "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + deling() + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"5.00\"},\"type\":" + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"5,00\"}},\"type\":"
                + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"(…) plus 5,00\"}}";
        String groep = groep(standaard(gevuld("gewogen gewicht"), plus), standaard(ALTIJD, EEN));
        geweigerd(groep, zaak(vondst("V1", "320.00", "240.00"), verhouding("V1", "5.75")), DELING);
    }

    // ---------------- documents ----------------

    private static final String TWEE_DECIMALEN = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static final String MODEL = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,"
            + "\"plural\":\"Vondsten\",\"attributes\":[" + attribuut("geschatte gewicht") + "," + attribuut("gewogen gewicht") + ","
            + attribuut("verhouding") + "],\"characteristics\":[]}],\"factTypes\":[],\"parameters\":[],\"domains\":[],"
            + "\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + TWEE_DECIMALEN + "}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    /** "zijn gewogen gewicht gedeeld door zijn geschatte gewicht". */
    private static String deling() {
        return "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + eigen("gewogen gewicht") + ",\"right\":"
                + eigen("geschatte gewicht") + ",\"type\":" + TWEE_DECIMALEN + ",\"source\":{\"snippet\":\"zijn gewogen gewicht"
                + " gedeeld door zijn geschatte gewicht\"}}";
    }

    /** "&lt;basis&gt; naar beneden afgerond op 2 decimalen". */
    private static String afgerond(String basis) {
        return "{\"node\":\"Rounding\",\"base\":" + basis + ",\"mode\":\"floor\",\"decimals\":2,\"type\":" + TWEE_DECIMALEN
                + ",\"source\":{\"snippet\":\"naar beneden afgerond op 2 decimalen\"}}";
    }

    private static String gevuld(String attribuut) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + eigen(attribuut)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribuut + " gevuld is\"}}";
    }

    private static String leegToets(String attribuut) {
        return "{\"node\":\"Predicate\",\"kind\":\"empty\",\"operand\":" + eigen(attribuut)
                + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"zijn " + attribuut + " leeg is\"}}";
    }

    private static String groterDanNul(String attribuut) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + eigen(attribuut) + ",\"right\":{\"node\":\"Literal\","
                + "\"value\":{\"number\":\"0\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"0\"}},\"source\":{\"snippet\":\"zijn " + attribuut + " is groter dan 0\"}}";
    }

    private static String alle(String... delen) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", delen)
                + "],\"source\":{\"snippet\":\"hij aan alle volgende voorwaarden voldoet\"}}";
    }

    private static final String ALTIJD = "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
            + "\"source\":{\"snippet\":\"altijd\"}}";

    /** The base "De verhouding van een Vondst moet gesteld worden op 1,00". */
    private static final String EEN = "{\"node\":\"Literal\",\"value\":{\"number\":\"1.00\"},\"type\":" + TWEE_DECIMALEN
            + ",\"source\":{\"snippet\":\"1,00\"}}";

    private static String standaard(String rechtvaardiging, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + gevolg + "}";
    }

    /** The group on the verhouding of a Vondst, highest default first. */
    private static String groep(String... standaarden) {
        String groep = "{\"node\":\"PrioritizedDefaults\",\"name\":\"verhouding\",\"subject\":\"Vondst\",\"attribute\":"
                + "\"verhouding\",\"defaults\":[" + String.join(",", standaarden) + "],\"validity\":{\"node\":\"Validity\"}}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[],\"dayKindDefinitions\":[],"
                + "\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + groep + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    /** A Vondst with an optional geschat and gewogen gewicht. */
    private static String vondst(String label, String geschat, String gewogen) {
        String a = (geschat == null ? "" : "\"geschatte gewicht\":{\"number\":\"" + geschat + "\"}")
                + (geschat != null && gewogen != null ? "," : "")
                + (gewogen == null ? "" : "\"gewogen gewicht\":{\"number\":\"" + gewogen + "\"}");
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{" + a + "},\"roles\":{},\"characteristics\":[]}";
    }

    private static String verhouding(String label, String waarde) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"verhouding\",\"value\":{\"number\":\""
                + waarde + "\"}}";
    }

    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String zaak(String instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + instanties
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The refusal of a division in a default on the truth-maintenance branch, after the prefix of its manifest. */
    private static final String DELING = "een deling in een default ('zijn gewogen gewicht gedeeld door zijn geschatte"
            + " gewicht'): deze tak rekent een default uit zodra wat hij leest staat, ook een die een hogere default"
            + " verdringt; een deling door nul daar is een fout op een tussenstand, die deze tak intrekt zodra de hogere"
            + " default beslist: of zo'n fout telt, wacht op een normbesluit (RUNTIME-SEMANTICS ch. 5.2, 7.1)";

    /** The truth-maintenance branch refuses the group {@code ir} by name, before {@code zaak} is replayed. */
    private static void geweigerd(String ir, String zaak, String vorm) throws Exception {
        Path tmp = Files.createTempFile("defaults-deling", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(tmp, c, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("defaults: defaults A:Vondst.verhouding — " + vorm), e.getMessage());
    }
}
