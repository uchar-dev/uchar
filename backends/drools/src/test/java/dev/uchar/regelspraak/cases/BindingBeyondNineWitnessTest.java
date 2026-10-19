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
class BindingBeyondNineWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void a_deviation_tree_on_the_inheritance_line_binds_thirteen_variables() throws Exception {
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
        // counts and two parameters: thirteen. ref and clingo: J 10-09-2026, S 20-10-2026 (the last lading), T 01-06-2026
        // (the vergunningseinde, written last and so highest), U 10-09-2026 (the inherited base), W 01-05-2026 (the
        // exception: 13 is above 10), X leeg; consistent.
        String meldtermijn = group("meldtermijn", "Jutter", defaultEntry(all(filled(own("vinddag", "Jutter")),
                greaterThan(count("Jutter", "wrakhout"), number(0))), plus(own("vinddag", "Jutter"), parameter("meldduur"))));
        String lading = all(filled(own("vinddag", "Strandjutter")), greaterThan(count("Strandjutter", "lading"), number(0)));
        String zwaar = defaultEntry(all(filled(own("vinddag", "Strandjutter")),
                        greaterThan(count("Strandjutter", "lading"), number(0)),
                        greaterThan(fold("sum", "gewicht", WHOLE, null), parameter("gewichtsdrempel"))),
                plus(fold("first", "bergdag", DATE, null), MAAND));
        String afterLading = "{\"node\":\"Default\",\"justification\":" + lading + ",\"consequence\":"
                + plus(fold("last", "bergdag", DATE, "lading"), parameter("meldduur")) + ",\"exceptions\":"
                + exceptions(zwaar) + "}";
        String toVergunning = defaultEntry(all(filled(own("vergunningseinde", "Strandjutter")),
                greaterThan(count("Strandjutter", "anker"), number(0))), own("vergunningseinde", "Strandjutter"));
        String forStrandjutter = group("meldtermijn voor Strandjutter", "Strandjutter",
                "{\"node\":\"Default\",\"exceptions\":" + exceptions(toVergunning, afterLading) + "}");
        String caseDocument = caseDocument(List.of(jutter("J", "Jutter", null), jutter("S", "Strandjutter", null),
                        jutter("T", "Strandjutter", "2026-06-01"), jutter("U", "Strandjutter", null),
                        jutter("W", "Strandjutter", null), jutter("X", "Strandjutter", null),
                        vondst("V1", "J", "wrakhout", null, 2), vondst("V2", "S", "lading", "2026-04-02", 3),
                        vondst("V3", "S", "lading", "2026-04-20", 4), vondst("V4", "T", "anker", null, 1),
                        vondst("V5", "T", "lading", "2026-05-05", 2), vondst("V6", "U", "wrakhout", null, 1),
                        vondst("V7", "W", "lading", "2026-04-01", 8), vondst("V8", "W", "lading", "2026-04-15", 5)),
                "\"meldduur\":{\"number\":\"6\"},\"gewichtsdrempel\":{\"number\":\"10\"}",
                List.of(date("J", "meldtermijn", "2026-09-10"), date("S", "meldtermijn", "2026-10-20"),
                        date("T", "meldtermijn", "2026-06-01"), date("U", "meldtermijn", "2026-09-10"),
                        date("W", "meldtermijn", "2026-05-01"), empty("X", "meldtermijn"), CONSISTENT));
        assertNull(play(document(List.of(), meldtermijn, forStrandjutter), caseDocument));
    }

    @Test void five_folds_in_the_value_of_a_rule() throws Exception {
        // "De meldtermijn van een Jutter moet berekend worden als de laatste van (de laatste van de bergdag van alle
        // vondsten van de Jutter die … "lading"), (de eerste van … "anker"), (de laatste van … "anker"), (de eerste van
        // … "wrakhout") en (de laatste van … "wrakhout")": five folds, each with its count, and the subject: eleven.
        // ref and clingo: J leeg (its wrakhout has no bergdag), S 20-04-2026, T 06-05-2026, X leeg.
        List<String> five = List.of(fold("last", "bergdag", DATE, "lading"), fold("first", "bergdag", DATE, "anker"),
                fold("last", "bergdag", DATE, "anker"), fold("first", "bergdag", DATE, "wrakhout"),
                fold("last", "bergdag", DATE, "wrakhout"));
        String value = "{\"node\":\"Aggregation\",\"kind\":\"last\",\"arguments\":[" + String.join(",", five)
                + "],\"type\":" + DATE + "}";
        String rule = "{\"node\":\"Rule\",\"name\":\"meldtermijn\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + own("meldtermijn", "Jutter") + ",\"value\":" + value
                + ",\"initialisation\":false,\"presentation\":\"calculation\"}]}";
        String caseDocument = caseDocument(List.of(jutter("J", "Jutter", null), jutter("S", "Strandjutter", null),
                        jutter("T", "Strandjutter", null), jutter("X", "Strandjutter", null),
                        vondst("V1", "J", "wrakhout", null, 2), vondst("V2", "S", "lading", "2026-04-02", 3),
                        vondst("V3", "S", "lading", "2026-04-20", 4), vondst("V4", "T", "anker", "2026-05-06", 1),
                        vondst("V5", "T", "wrakhout", "2026-03-30", 2), vondst("V6", "T", "lading", "2026-04-11", 2)),
                "", List.of(empty("J", "meldtermijn"), date("S", "meldtermijn", "2026-04-20"),
                        date("T", "meldtermijn", "2026-05-06"), empty("X", "meldtermijn")));
        assertNull(play(document(List.of(rule)), caseDocument));
    }

    @Test void a_uniqueness_over_nine_sources() throws Exception {
        // "Het vondstnummer van Strandvondst verenigd met Wrakvondst, …, Plankvondst moeten uniek zijn": per source
        // a count of the instances of every source with the same number, so the consequence binds the subject and
        // nine counts. ref and clingo: S1 and P1 share 7 and are inconsistent; W1 (8) is not. A flank, green before and
        // after: past eight sources the counts were folded pairwise into one running total, which this binding replaces.
        List<String> kinds = List.of("Strandvondst", "Wrakvondst", "Ankervondst", "Netvondst", "Tonvondst", "Kistvondst",
                "Flesvondst", "Touwvondst", "Plankvondst");
        List<String> types = new ArrayList<>(), sources = new ArrayList<>();
        for (String t : kinds) {
            types.add("{\"node\":\"ObjectType\",\"name\":\"" + t + "\",\"animate\":false,\"plural\":\"" + t + "en\","
                    + "\"attributes\":[" + attribute("vondstnummer", WHOLE) + "],\"characteristics\":[]}");
            sources.add("{\"node\":\"Source\",\"objectType\":\"" + t + "\",\"keys\":[\"vondstnummer\"]}");
        }
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[" + String.join(",", types) + "],\"factTypes\":[],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[{\"node\":\"Uniqueness\",\"name\":\"eenmalig vondstnummer\","
                + "\"versions\":[{\"sources\":[" + String.join(",", sources) + "],\"validity\":{\"node\":\"Validity\"}}]}],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                + "\"diagnostics\":[]}";
        String caseDocument = caseDocument(List.of(numbered("S1", "Strandvondst", 7), numbered("W1", "Wrakvondst", 8),
                        numbered("P1", "Plankvondst", 7)), "",
                List.of(inconsistent("S1"), inconsistent("P1")));
        assertNull(play(ir, caseDocument));
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String WHOLE = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String NIET_NEGATIEF = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String MND = "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String MAANDEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\","
            + "\"unit\":" + MND + "}";
    /** "1 mnd". */
    private static final String MAAND = "{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":" + MAANDEN + "}";
    private static final String VONDSTROL = "{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}";

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static String parameter(String name) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"" + name + "\"}";
    }

    private static String number(int n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + WHOLE + "}";
    }

    private static String plus(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + DATE + "}";
    }

    private static String filled(String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"filled\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0}";
    }

    private static String greaterThan(String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":" + left + ",\"right\":" + right + "}";
    }

    private static String all(String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[" + String.join(",", parts) + "]}";
    }

    /** "die aan de volgende voorwaarde voldoen: zijn soort is gelijk aan "&lt;soort&gt;"", or no filter for null. */
    private static String kindFilter(String category) {
        return category == null ? "" : ",\"filter\":{\"node\":\"Compound\",\"quantifier\":\"the\",\"threshold\":0,\"parts\":["
                + "{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":" + own("soort", "Vondst") + ",\"right\":{\"node\":"
                + "\"Literal\",\"value\":{\"text\":\"" + category + "\"},\"type\":{\"node\":\"Text\"}}}]}";
    }

    /** "de &lt;kind&gt; van de &lt;lid&gt; van alle vondsten van de Jutter [die … &lt;soort&gt;]". */
    private static String fold(String kind, String member, String type, String category) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + own(member, "Vondst")
                + "],\"elementType\":\"Vondst\"" + kindFilter(category) + ",\"sourceRole\":" + VONDSTROL + ",\"type\":" + type + "}";
    }

    /** "het aantal vondsten van de &lt;type&gt; die … &lt;soort&gt;". */
    private static String count(String type, String category) {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + own("vondsten", type)
                + "],\"elementType\":\"Vondst\"" + kindFilter(category) + ",\"sourceRole\":" + VONDSTROL + ",\"type\":"
                + NIET_NEGATIEF + "}";
    }

    private static String defaultEntry(String justification, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + justification + ",\"consequence\":" + consequence + "}";
    }

    /** One ordered exception group, highest first. */
    private static String exceptions(String... defaults) {
        return "{\"unordered\":false,\"members\":[{\"node\":\"ExceptionGroup\",\"defaults\":[" + String.join(",", defaults)
                + "]}]}";
    }

    private static String group(String name, String type, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + name + "\",\"subject\":\"" + type + "\",\"attribute\":"
                + "\"meldtermijn\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    private static String attribute(String name, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + name + "\",\"datatype\":" + datatype + "}";
    }

    /** Jutter, its subtype Strandjutter and the Vondst, JutterHeeftVondst, the parameters meldduur (mnd) and
     *  gewichtsdrempel, and the unit system Tijd. */
    private static String document(List<String> rules, String... groups) {
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[" + attribute("vinddag", DATE) + "," + attribute("meldtermijn", DATE) + "],\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Strandjutter\",\"supertype\":\"Jutter\",\"animate\":false,\"plural\":"
                + "\"Strandjutters\",\"attributes\":[" + attribute("vergunningseinde", DATE) + "],\"characteristics\":[]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribute("soort", "{\"node\":\"Text\"}") + "," + attribute("bergdag", DATE) + "," + attribute("gewicht", WHOLE)
                + "],\"characteristics\":[]}],"
                + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"jutter\",\"objectType\":\"Jutter\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\",\"plural\":\"vondsten\",\"many\":true}]}],"
                + "\"parameters\":[{\"node\":\"Parameter\",\"name\":\"meldduur\",\"datatype\":" + MAANDEN + "},"
                + "{\"node\":\"Parameter\",\"name\":\"gewichtsdrempel\",\"datatype\":" + WHOLE + "}],\"domains\":[],"
                + "\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\","
                + "\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]},"
                + "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\",\"factorToCanonical\":"
                + "{\"num\":1,\"den\":1},\"spellings\":[\"maand\"]}]}],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groups) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static final String CONSISTENT = "{\"kind\":\"consistent\"}";

    private static String jutter(String label, String type, String vergunningseinde) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{\"vinddag\":{\"date\":\"2026-03-10\"}"
                + (vergunningseinde == null ? "" : ",\"vergunningseinde\":{\"date\":\"" + vergunningseinde + "\"}")
                + "},\"roles\":{},\"characteristics\":[]}";
    }

    private static String vondst(String label, String jutter, String category, String bergdag, int weight) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"Vondst\",\"attributes\":{\"soort\":{\"text\":\"" + category + "\"}"
                + (bergdag == null ? "" : ",\"bergdag\":{\"date\":\"" + bergdag + "\"}") + ",\"gewicht\":{\"number\":\""
                + weight + "\"}},\"roles\":{\"jutter\":[\"" + jutter + "\"]},\"characteristics\":[]}";
    }

    private static String numbered(String label, String type, int ordinal) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{\"vondstnummer\":{\"number\":\""
                + ordinal + "\"}},\"roles\":{},\"characteristics\":[]}";
    }

    private static String inconsistent(String label) {
        return "{\"kind\":\"inconsistent\",\"rule\":\"eenmalig vondstnummer\",\"label\":\"" + label + "\"}";
    }

    private static String date(String label, String attribute, String iso) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute
                + "\",\"value\":{\"date\":\"" + iso + "\"}}";
    }

    private static String empty(String label, String attribute) {
        return "{\"kind\":\"attributeEmpty\",\"label\":\"" + label + "\",\"attribute\":\"" + attribute + "\"}";
    }

    private static String caseDocument(List<String> instances, String parameters, List<String> expected) {
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instances)
                + "],\"parameters\":{" + parameters + "}},\"expected\":[" + String.join(",", expected) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("binding-voorbij-negen", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), false);
    }
}
