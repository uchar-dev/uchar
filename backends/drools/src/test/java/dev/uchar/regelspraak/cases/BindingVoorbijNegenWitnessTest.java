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
 * A rule whose consequence reads more than nine bound variables: the subject, and per fold, count or sum its result
 * and, where an empty source must read leeg, its contributor count, and per parameter its global. A defaults group
 * binds what every default of its effective stack reads (RUNTIME-SEMANTICS ch. 7.10), so a tree whose justifications
 * count and add and whose consequences fold over dates passes nine quickly; a rule's value and a creation's
 * toekenningen do so with five folds. The value is the one ref and clingo answer: the number of variables a
 * consequence binds is a property of this branch's lowering, not of the document. Every document is authored in this
 * class on Jutterswet material, with the values {@code ref} and clingo answer.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): each row was
 * refused for the whole group or rule, {@code te veel gebonden variabelen (aggregaties, vouwen en globals)} for the
 * group and {@code regel (THEN-waarde: te veel gebonden globals en vouwen)} for the rule.
 */
class BindingVoorbijNegenWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void een_afwijkingsboom_op_de_erflijn_bindt_twaalf_variabelen() throws Exception {
        // Regel meldtermijn: "De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus de meldduur indien
        // hij aan alle volgende voorwaarden voldoet: zijn vinddag is gevuld; het aantal vondsten van de Jutter die aan de
        // volgende voorwaarde voldoen: zijn soort is gelijk aan "wrakhout" is groter dan 0".
        // Afwijking van meldtermijn: meldtermijn na de laatste berging: "De meldtermijn van een Strandjutter moet berekend
        // worden als (de laatste van de bergdag van alle vondsten van de Strandjutter die … "lading") plus de meldduur
        // indien … zijn vinddag is gevuld; het aantal vondsten van de Strandjutter die … "lading" is groter dan 0".
        // Uitzondering op meldtermijn na de laatste berging: zware lading: "… (de eerste van de bergdag van alle vondsten
        // van de Strandjutter) plus 1 mnd indien de som van het gewicht van alle vondsten van de Strandjutter groter is
        // dan de gewichtsdrempel", with the condition of the afwijking it is written under.
        // Afwijking van meldtermijn: meldtermijn tot het vergunningseinde: "… gesteld worden op zijn vergunningseinde
        // indien … zijn vergunningseinde is gevuld; het aantal vondsten van de Strandjutter die … "anker" is groter dan 0".
        // The stack of a Strandjutter binds the subject, four counts, one sum with its count, two date folds with their
        // counts and two parameters: twelve. ref and clingo: J 10-09-2026, S 20-10-2026 (the last lading), T 01-06-2026
        // (the vergunningseinde, written last and so highest), U 10-09-2026 (the inherited base), W 01-05-2026 (the
        // exception: 13 is above 10), X leeg; consistent.
        String meldtermijn = groep("meldtermijn", "Jutter", standaard(alle(gevuld(eigen("vinddag", "Jutter")),
                groterDan(telling("Jutter", "wrakhout"), getal(0))), plus(eigen("vinddag", "Jutter"), parameter("meldduur"))));
        String lading = alle(gevuld(eigen("vinddag", "Strandjutter")), groterDan(telling("Strandjutter", "lading"), getal(0)));
        String zwaar = standaard(alle(gevuld(eigen("vinddag", "Strandjutter")),
                        groterDan(telling("Strandjutter", "lading"), getal(0)),
                        groterDan(vouw("sum", "gewicht", GEHEEL, null), parameter("gewichtsdrempel"))),
                plus(vouw("first", "bergdag", DATUM, null), MAAND));
        String naLading = "{\"node\":\"Default\",\"justification\":" + lading + ",\"consequence\":"
                + plus(vouw("last", "bergdag", DATUM, "lading"), parameter("meldduur")) + ",\"exceptions\":"
                + uitzonderingen(zwaar) + "}";
        String totVergunning = standaard(alle(gevuld(eigen("vergunningseinde", "Strandjutter")),
                groterDan(telling("Strandjutter", "anker"), getal(0))), eigen("vergunningseinde", "Strandjutter"));
        String voorStrandjutter = groep("meldtermijn voor Strandjutter", "Strandjutter",
                "{\"node\":\"Default\",\"exceptions\":" + uitzonderingen(totVergunning, naLading) + "}");
        String zaak = zaak(List.of(jutter("J", "Jutter", null), jutter("S", "Strandjutter", null),
                        jutter("T", "Strandjutter", "2026-06-01"), jutter("U", "Strandjutter", null),
                        jutter("W", "Strandjutter", null), jutter("X", "Strandjutter", null),
                        vondst("V1", "J", "wrakhout", null, 2), vondst("V2", "S", "lading", "2026-04-02", 3),
                        vondst("V3", "S", "lading", "2026-04-20", 4), vondst("V4", "T", "anker", null, 1),
                        vondst("V5", "T", "lading", "2026-05-05", 2), vondst("V6", "U", "wrakhout", null, 1),
                        vondst("V7", "W", "lading", "2026-04-01", 8), vondst("V8", "W", "lading", "2026-04-15", 5)),
                "\"meldduur\":{\"number\":\"6\"},\"gewichtsdrempel\":{\"number\":\"10\"}",
                List.of(datum("J", "meldtermijn", "2026-09-10"), datum("S", "meldtermijn", "2026-10-20"),
                        datum("T", "meldtermijn", "2026-06-01"), datum("U", "meldtermijn", "2026-09-10"),
                        datum("W", "meldtermijn", "2026-05-01"), leeg("X", "meldtermijn"), CONSISTENT));
        assertNull(speel(document(List.of(), meldtermijn, voorStrandjutter), zaak));
    }

    @Test void vijf_vouwen_in_de_waarde_van_een_regel() throws Exception {
        // "De meldtermijn van een Jutter moet berekend worden als de laatste van (de laatste van de bergdag van alle
        // vondsten van de Jutter die … "lading"), (de eerste van … "anker"), (de laatste van … "anker"), (de eerste van
        // … "wrakhout") en (de laatste van … "wrakhout")": five folds, each with its count, and the subject: eleven.
        // ref and clingo: J leeg (its wrakhout has no bergdag), S 20-04-2026, T 06-05-2026, X leeg.
        List<String> vijf = List.of(vouw("last", "bergdag", DATUM, "lading"), vouw("first", "bergdag", DATUM, "anker"),
                vouw("last", "bergdag", DATUM, "anker"), vouw("first", "bergdag", DATUM, "wrakhout"),
                vouw("last", "bergdag", DATUM, "wrakhout"));
        String waarde = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + String.join(",", vijf)
                + "],\"type\":" + DATUM + "}";
        String regel = "{\"node\":\"Rule\",\"name\":\"meldtermijn\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + eigen("meldtermijn", "Jutter") + ",\"value\":" + waarde
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        String zaak = zaak(List.of(jutter("J", "Jutter", null), jutter("S", "Strandjutter", null),
                        jutter("T", "Strandjutter", null), jutter("X", "Strandjutter", null),
                        vondst("V1", "J", "wrakhout", null, 2), vondst("V2", "S", "lading", "2026-04-02", 3),
                        vondst("V3", "S", "lading", "2026-04-20", 4), vondst("V4", "T", "anker", "2026-05-06", 1),
                        vondst("V5", "T", "wrakhout", "2026-03-30", 2), vondst("V6", "T", "lading", "2026-04-11", 2)),
                "", List.of(leeg("J", "meldtermijn"), datum("S", "meldtermijn", "2026-04-20"),
                        datum("T", "meldtermijn", "2026-05-06"), leeg("X", "meldtermijn")));
        assertNull(speel(document(List.of(regel)), zaak));
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String MND = "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String MAANDEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\","
            + "\"unit\":" + MND + "}";
    /** "1 mnd". */
    private static final String MAAND = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + MAANDEN + "}";
    private static final String VONDSTROL = "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static String parameter(String naam) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + naam + "\"}";
    }

    private static String getal(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + "}";
    }

    private static String plus(String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + DATUM + "}";
    }

    private static String gevuld(String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0}";
    }

    private static String groterDan(String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + links + ",\"right\":" + rechts + "}";
    }

    private static String alle(String... delen) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", delen) + "]}";
    }

    /** "die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan "&lt;soort&gt;"", or no filter for null. */
    private static String soortfilter(String soort) {
        return soort == null ? "" : ",\"filter\":{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":["
                + "{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":" + eigen("soort", "Vondst") + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"text\":\"" + soort + "\"},\"type\":{\"node\":\"Text\"}}}]}";
    }

    /** "de &lt;kind&gt; van de &lt;lid&gt; van alle vondsten van de Jutter [die … &lt;soort&gt;]". */
    private static String vouw(String kind, String lid, String type, String soort) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + eigen(lid, "Vondst")
                + "],\"elementType\":\"Vondst\"" + soortfilter(soort) + ",\"sourceRole\":" + VONDSTROL + ",\"type\":" + type + "}";
    }

    /** "het aantal vondsten van de &lt;type&gt; die … &lt;soort&gt;". */
    private static String telling(String type, String soort) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + eigen("vondsten", type)
                + "],\"elementType\":\"Vondst\"" + soortfilter(soort) + ",\"sourceRole\":" + VONDSTROL + ",\"type\":"
                + NIET_NEGATIEF + "}";
    }

    private static String standaard(String rechtvaardiging, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + gevolg + "}";
    }

    /** One ordered exception group, highest first. */
    private static String uitzonderingen(String... defaults) {
        return "{\"unordered\":false,\"members\":[{\"node\":\"ExceptionGroup\",\"defaults\":[" + String.join(",", defaults)
                + "]}]}";
    }

    private static String groep(String naam, String type, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"attribute\":"
                + "\"meldtermijn\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    /** Jutter, its subtype Strandjutter and the Vondst, JutterHeeftVondst, the parameters meldduur (mnd) and
     *  gewichtsdrempel, and the unit system Tijd. */
    private static String document(List<String> regels, String... groepen) {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[" + attribuut("vinddag", DATUM) + "," + attribuut("meldtermijn", DATUM) + "],\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Strandjutter\",\"supertype\":\"Jutter\",\"animate\":false,\"plural\":"
                + "\"Strandjutters\",\"attributes\":[" + attribuut("vergunningseinde", DATUM) + "],\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribuut("soort", "{\"node\":\"Text\"}") + "," + attribuut("bergdag", DATUM) + "," + attribuut("gewicht", GEHEEL)
                + "],\"characteristics\":[]}],"
                + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],"
                + "\"parameters\":[{\"node\":\"Parameter\",\"name\":\"meldduur\",\"datatype\":" + MAANDEN + "},"
                + "{\"node\":\"Parameter\",\"name\":\"gewichtsdrempel\",\"datatype\":" + GEHEEL + "}],\"domains\":[],"
                + "\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\","
                + "\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]},"
                + "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\",\"factorToCanonical\":"
                + "{\"num\":1,\"den\":1},\"spellings\":[\"maand\"]}]}],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groepen) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String jutter(String label, String type, String vergunningseinde) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{\"vinddag\":{\"date\":\"2026-03-10\"}"
                + (vergunningseinde == null ? "" : ",\"vergunningseinde\":{\"date\":\"" + vergunningseinde + "\"}")
                + "},\"roles\":{},\"characteristics\":[]}";
    }

    private static String vondst(String label, String jutter, String soort, String bergdag, int gewicht) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"soort\":{\"text\":\"" + soort + "\"}"
                + (bergdag == null ? "" : ",\"bergdag\":{\"date\":\"" + bergdag + "\"}") + ",\"gewicht\":{\"number\":\""
                + gewicht + "\"}},\"roles\":{\"jutter\":[\"" + jutter + "\"]},\"characteristics\":[]}";
    }

    private static String datum(String label, String attribuut, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut
                + "\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    private static String leeg(String label, String attribuut) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribuut + "\"}";
    }

    private static String zaak(List<String> instanties, String parameters, List<String> verwacht) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{" + parameters + "}},\"expected\":[" + String.join(",", verwacht) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("binding-voorbij-negen", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.speelCase(tmp, JSON.readTree(zaak), false);
    }
}
