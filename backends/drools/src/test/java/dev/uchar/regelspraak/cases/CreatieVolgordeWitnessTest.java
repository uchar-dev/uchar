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
class CreatieVolgordeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_toekenning_leest_een_waarde_die_een_defaultsgroep_schrijft() throws Exception {
        // "… met telling gelijk aan zijn basis", with "De basis van een Jutter moet berekend worden als 2" and "Afwijking
        // van basis: opgegeven basis gaat voor …"; J1's opgegeven basis is 7. ref and clingo: 7, 2, 2 and 2.
        String groep = groep("basis", "Jutter", eigen("opgegeven basis", "Jutter"), getal(2));
        String ir = document(List.of(creatie(eigen("basis", "Jutter"))), List.of(LEES_TERUG), List.of(groep), List.of());
        List<String> situatie = new ArrayList<>(situatie());
        situatie.set(0, instantie("J1", "Jutter", "\"opgegeven basis\":{\"number\":\"7\"}", ""));
        assertNull(speel(ir, zaak(situatie, telling("7", "2", "2", "2"))), "de basis uit een defaultsgroep");
    }

    @Test void een_vouw_over_een_lid_dat_een_defaultsgroep_schrijft() throws Exception {
        // "… met telling gelijk aan de som van het nettogewicht van alle vondsten van de Jutter", the nettogewicht by a
        // group: zijn gewicht min 1, or the opgegeven nettogewicht where it is filled (V1: 10). ref and clingo: J1 10 + 6
        // + (leeg min 1 = -1) = 15, J2 leeg, J3 -1 and J4 8.
        String groep = groep("nettogewicht", "Vondst", eigen("opgegeven nettogewicht", "Vondst"),
                binair("minus", eigen("gewicht", "Vondst"), getal(1)));
        String ir = document(List.of(creatie(vouw("sum", "nettogewicht", null))), List.of(LEES_TERUG), List.of(groep), List.of());
        List<String> situatie = new ArrayList<>(situatie());
        situatie.set(4, situatie.get(4).replace("\"attributes\":{", "\"attributes\":{\"opgegeven nettogewicht\":{\"number\":\"10\"},"));
        assertNull(speel(ir, zaak(situatie, telling("15", null, "-1", "8"))), "een som over een lid uit een defaultsgroep");
    }

    @Test void een_vouw_over_een_kenmerk_dat_een_ontkenning_afleidt() throws Exception {
        // "… met telling gelijk aan het aantal vondsten van de Jutter die licht zijn", with "Een Vondst is licht indien hij
        // niet zwaar is" and "Een Vondst is zwaar indien zijn gewicht groter is dan 5": the stratification places the
        // negation below the other rules. ref and clingo: 2 (V1 and V3), 0, 1 and 0.
        String zwaar = kenmerkRegel("zwaar", "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + eigen("gewicht", "Vondst")
                + ",\"right\":" + getal(5) + ",\"source\":{\"snippet\":\"zijn gewicht groter is dan 5\"}}");
        String licht = kenmerkRegel("licht", "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet zwaar is\"}}");
        String telling = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}],\"elementType\":\"Vondst\","
                + "\"filter\":{\"node\":\"CharacteristicCheck\",\"characteristic\":\"licht\",\"negated\":false,\"source\":{\"snippet\":"
                + "\"licht zijn\"}},\"sourceRole\":" + rolbinding("JutterHeeftVondst", "vondst") + ",\"type\":" + TELGETAL
                + ",\"source\":{\"snippet\":\"het aantal vondsten van de Jutter die licht zijn\"}}";
        String ir = document(List.of(creatie(telling)), List.of(LEES_TERUG, licht, zwaar), List.of(), List.of());
        assertNull(speel(ir, zaak(situatie(), telling("2", "0", "1", "0"))), "een telling over een ontkend kenmerk");
    }

    @Test void een_telling_over_wat_een_latere_creatie_munt() throws Exception {
        // "… met telling gelijk aan het aantal netten van de Jutter", and after it "Een Jutter heeft een net met maas
        // gelijk aan 5". ref and clingo: 1 for every Jutter.
        String netten = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"netten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"netten van de Jutter\"}}],\"elementType\":\"Net\","
                + "\"sourceRole\":" + rolbinding("JutterHeeftNet", "net") + ",\"type\":" + TELGETAL
                + ",\"source\":{\"snippet\":\"het aantal netten van de Jutter\"}}";
        String maakNet = "{\"node\":\"ObjectCreation\",\"name\":\"maak een net\",\"versions\":[{\"createdType\":\"Net\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"maas\",\"value\":" + getal(5)
                + ",\"characteristic\":false}],\"validity\":{\"node\":\"Validity\"},\"connectingRole\":" + rolbinding("JutterHeeftNet", "net")
                + "}]}";
        String ir = document(List.of(creatie(netten), maakNet), List.of(LEES_TERUG), List.of(), List.of());
        assertNull(speel(ir, zaak(situatie(), telling("1", "1", "1", "1"))), "een telling over de netten van een latere creatie");
    }

    @Test void een_telling_over_een_relatie_die_een_feitcreatie_legt() throws Exception {
        // "… met telling gelijk aan het aantal gecontroleerde netten van de Jutter", with "Een gecontroleerd net van een
        // Jutter is een net van de Jutter"; each Jutter has one net. ref and clingo: 1 for every Jutter.
        String gecontroleerd = "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"gecontroleerde netten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"gecontroleerde netten van de Jutter\"}}],"
                + "\"elementType\":\"Net\",\"sourceRole\":" + rolbinding("JutterControleertNet", "gecontroleerd net") + ",\"type\":"
                + TELGETAL + ",\"source\":{\"snippet\":\"het aantal gecontroleerde netten van de Jutter\"}}";
        String controleer = "{\"node\":\"FactCreation\",\"name\":\"controleer\",\"versions\":[{\"factType\":\"JutterControleertNet\","
                + "\"anchorRole\":\"controleur\",\"targetRole\":\"gecontroleerd net\",\"anchor\":\"Jutter\",\"path\":["
                + rolbinding("JutterHeeftNet", "net") + "],\"validity\":{\"node\":\"Validity\"}}]}";
        String ir = document(List.of(creatie(gecontroleerd)), List.of(LEES_TERUG), List.of(), List.of(controleer));
        List<String> situatie = new ArrayList<>(situatie());
        for (int i = 1; i <= 4; i++) situatie.add(instantie("N" + i, "Net", "", "\"eigenaar\":[\"J" + i + "\"]"));
        assertNull(speel(ir, zaak(situatie, telling("1", "1", "1", "1"))), "een telling over een relatie uit een feitcreatie");
    }

    // ---------------- the flank ----------------

    @Test void een_gewone_regel_na_de_creatie_vuurt_toch_eerder() throws Exception {
        // "… met telling gelijk aan de som van het nettogewicht van alle vondsten van de Jutter", with "Het nettogewicht
        // van een Vondst moet berekend worden als zijn gewicht min 1" declared after the creation. ref and clingo: 8,
        // leeg, -1 and 8.
        String netto = "{\"node\":\"Rule\",\"name\":\"nettogewicht\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"nettogewicht\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Het nettogewicht van een Vondst\"}},\"value\":" + binair("minus", eigen("gewicht", "Vondst"), getal(1))
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        String ir = document(List.of(creatie(vouw("sum", "nettogewicht", null))), List.of(LEES_TERUG, netto), List.of(), List.of());
        assertNull(speel(ir, zaak(situatie(), telling("8", null, "-1", "8"))), "een som over een lid uit een latere regel");
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String TELGETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";

    private static String attribuut(String naam) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + GEHEEL + "}";
    }

    private static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"nominal\"}";
    }

    private static String objecttype(String naam, String meervoud, boolean bezield, List<String> kenmerken, String... leden) {
        List<String> a = new ArrayList<>();
        for (String l : leden) a.add(attribuut(l));
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", a) + "],\"characteristics\":[" + String.join(",", kenmerken) + "]}";
    }

    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\""
                + (veelMeervoud == null ? ",\"many\":false}" : ",\"plural\":\"" + veelMeervoud + "\",\"many\":true}") + "]}";
    }

    /** Jutter, Afrekening, Vondst and Net; one jutter has many vondsten, one afrekening, many netten and many
     *  gecontroleerde netten. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Jutter", "Jutters", true, List.of(), "telling", "basis", "opgegeven basis") + ","
            + objecttype("Afrekening", "Afrekeningen", false, List.of(), "telling") + ","
            + objecttype("Vondst", "Vondsten", false, List.of(kenmerk("zwaar"), kenmerk("licht")), "gewicht", "nettogewicht",
                    "opgegeven nettogewicht") + ","
            + objecttype("Net", "Netten", false, List.of(), "maas")
            + "],\"factTypes\":["
            + feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + feittype("JutterHeeftAfrekening", "afrekenaar", "Jutter", "afrekening", null, "Afrekening") + ","
            + feittype("JutterHeeftNet", "eigenaar", "Jutter", "net", "netten", "Net") + ","
            + feittype("JutterControleertNet", "controleur", "Jutter", "gecontroleerd net", "gecontroleerde netten", "Net")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String rolbinding(String feittype, String rol) {
        return "{\"node\":\"RoleBinding\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\"}";
    }

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type + "\",\"source\":"
                + "{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"" + n + "\"}}";
    }

    private static String binair(String op, String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + GEHEEL
                + ",\"source\":{\"snippet\":\"" + op + "\"}}";
    }

    /** A fold of kind {@code soort} over the {@code lid} of the vondsten of the Jutter; {@code filter} null for none. */
    private static String vouw(String soort, String lid, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + soort + "\",\"arguments\":[{\"node\":\"AttributeReference\",\"attribute\":\""
                + lid + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":\"het " + lid + " van alle vondsten van de Jutter\"}}],"
                + "\"elementType\":\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":"
                + rolbinding("JutterHeeftVondst", "vondst") + ",\"type\":" + GEHEEL + ",\"source\":{\"snippet\":\"de som van het " + lid
                + " van alle vondsten van de Jutter\"}}";
    }

    /** "Regel maak een afrekening: Een Jutter heeft een afrekening met telling gelijk aan &lt;waarde&gt;." */
    private static String creatie(String waarde) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"maak een afrekening\",\"versions\":[{\"createdType\":\"Afrekening\","
                + "\"subject\":\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"telling\",\"value\":" + waarde
                + ",\"characteristic\":false}],\"validity\":{\"node\":\"Validity\"},\"connectingRole\":"
                + rolbinding("JutterHeeftAfrekening", "afrekening") + "}]}";
    }

    /** "De telling van een Jutter moet berekend worden als de telling van de afrekening van de Jutter." */
    private static final String LEES_TERUG = "{\"node\":\"Rule\",\"name\":\"lees telling terug\",\"versions\":[{\"validity\":"
            + "{\"node\":\"Validity\"},\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"telling\",\"subjectType\":\"Jutter\","
            + "\"source\":{\"snippet\":\"De telling van een Jutter\"}},\"value\":{\"node\":\"AttributeReference\",\"attribute\":"
            + "\"telling\",\"subjectType\":\"Afrekening\",\"source\":{\"snippet\":\"de telling van de afrekening van de Jutter\"},"
            + "\"path\":[" + rolbinding("JutterHeeftAfrekening", "afrekening") + "]},\"initialisation\":false,\"presentation\":"
            + "\"calculation\"}]}";

    /** "Een Vondst is &lt;kenmerk&gt; indien &lt;voorwaarde&gt;." */
    private static String kenmerkRegel(String kenmerk, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + kenmerk + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + "{\"node\":\"AttributeReference\",\"attribute\":\"" + kenmerk + "\",\"subjectType\":\"Vondst\",\"source\":{\"snippet\":"
                + "\"Een Vondst is " + kenmerk + "\"}},\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":"
                + "\"Boolean\"},\"source\":{\"snippet\":\"waar\"}},\"condition\":" + voorwaarde + ",\"initialisation\":false}]}";
    }

    /** A defaults group on {@code lid} of {@code type}: the opgave where it is filled, else {@code basis}. */
    private static String groep(String lid, String type, String opgave, String basis) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + lid + "\",\"subject\":\"" + type + "\",\"attribute\":\"" + lid
                + "\",\"defaults\":[{\"node\":\"Default\",\"justification\":{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":"
                + opgave + ",\"negated\":false,\"digits\":0,\"source\":{\"snippet\":\"de opgave gevuld is\"}},\"consequence\":" + opgave
                + "},{\"node\":\"Default\",\"justification\":{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],"
                + "\"source\":{\"snippet\":\"altijd\"}},\"consequence\":" + basis + "}],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String document(List<String> creaties, List<String> regels, List<String> groepen, List<String> feitcreaties) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creaties) + "],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[" + String.join(",", feitcreaties) + "],"
                + "\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[" + String.join(",", groepen) + "]},"
                + "\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String vondst(String label, String gewicht, String jutter) {
        return instantie(label, "Vondst", gewicht == null ? "" : "\"gewicht\":{\"number\":\"" + gewicht + "\"}",
                "\"jutter\":[\"" + jutter + "\"]");
    }

    /** The situation of the class comment: J1 to J4, then V1 to V5. */
    private static List<String> situatie() {
        return List.of(instantie("J1", "Jutter", "", ""), instantie("J2", "Jutter", "", ""), instantie("J3", "Jutter", "", ""),
                instantie("J4", "Jutter", "", ""), vondst("V1", "4", "J1"), vondst("V2", "7", "J1"), vondst("V3", null, "J1"),
                vondst("V4", null, "J3"), vondst("V5", "9", "J4"));
    }

    /** The telling of J1 to J4, a null value leeg. */
    private static String telling(String... waarden) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < waarden.length; i++) {
            String label = "J" + (i + 1);
            uit.add(waarden[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"telling\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"telling\",\"value\":{\"number\":\""
                            + waarden[i] + "\"}}");
        }
        return String.join(",", uit);
    }

    private static String zaak(List<String> instanties, String verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + verwacht + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return speel(ir, zaak, false);
    }

    private static String speel(String ir, String zaak, boolean drl) throws Exception {
        Path tmp = Files.createTempFile("creatie-volgorde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        JsonNode c = JSON.readTree(zaak);
        return CaseRunner.speelCase(tmp, c, drl);
    }
}
