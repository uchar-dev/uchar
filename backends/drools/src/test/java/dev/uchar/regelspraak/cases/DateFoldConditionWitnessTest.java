package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A date folded over a set source as an operand of a condition and of a justification: "de laatste van de bergdag van
 * alle vondsten van de Jutter later is dan zijn meldtermijn", the fold on either side, inside date arithmetic with a
 * literal duration or a parameter, compared for equality and tested for leeg, one and two roles away, under each
 * quantifier, in the condition of a rule that writes a characteristic or an attribute and in the justification of the
 * base, an exception and a deviation of a defaults tree. RUNTIME-SEMANTICS ch. 1.2e: the ordering fold selects one
 * contributed date, and is leeg over an empty source or where no contributor has a date; ch. 1.2 then gives the empty
 * operand of the comparison and of the arithmetic around it. The same fold as a rule's value plays since a fold became
 * an accumulate beside the subject pattern. Every document here is authored in this class on Jutterswet material, with
 * the values {@code ref} and clingo answer.
 *
 * <p>Situation (each case): K1 (sluitdag 15-04-2026) watches J1 and J2, K2 (15-04-2026) J3, K3 (15-04-2026) none, K4
 * (no sluitdag) J5. J1 (vinddag 10-03-2026, meldtermijn 10-04-2026) found V1 (lading, bergdag 02-04-2026) and V2 (anker,
 * 20-04-2026); J2 (the same dates) found nothing; J3 (vinddag 10-03-2026, meldtermijn 10-05-2026) found V3 (lading,
 * 01-04-2026) and V4 (anker, no bergdag); J4 (no vinddag, meldtermijn 01-04-2026) found V5 (lading, 01-04-2026); J5
 * (vinddag 10-03-2026, no meldtermijn) found V6 (anker, 05-04-2026). The meldduur is 14 dagen, the peildatum
 * 03-04-2026.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): a condition
 * refused the rule ({@code voorwaarde met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum
 * over een rol), die deze tak alleen als waarde vouwt …}) and a justification the group ({@code rechtvaardiging niet
 * vertaald, een operand die deze tak niet als waarde leest: …}; for a minimum or maximum {@code … een vergelijking van
 * een aggregatie met iets anders dan een getal …}). The rows over a date parameter and over a member a later rule
 * derives came after the fix and were red against the transpiler of the claim in the same sentences.
 */
class DateFoldConditionWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the condition of a rule ----------------

    @Test void the_last_bergdag_left_and_right() throws Exception {
        // "Een Jutter heeft de late melding indien de laatste van de bergdag van alle vondsten van de Jutter later is
        // dan zijn meldtermijn", and "indien zijn meldtermijn eerder is dan de laatste van …". ref and clingo: J1 only.
        String caseDocument = caseDocument(kenmerken("late melding", true, false, false, false, false));
        assertNull(play(document(characteristicRule("late melding", compare("later", fold("last"), MELDTERMIJN))), caseDocument), "links");
        assertNull(play(document(characteristicRule("late melding", compare("earlier", MELDTERMIJN, fold("last")))), caseDocument), "rechts");
    }

    @Test void with_a_date_arithmetic_and_a_parameter() throws Exception {
        // "(de laatste van …) plus 14 dg later is dan zijn meldtermijn", the same with "plus de meldduur", and "zijn
        // meldtermijn eerder is dan (de eerste van …) plus 14 dg". ref and clingo: J1 and J4, the last two J1 and J4
        // as well (J3 counts from 01-04-2026 to 15-04-2026, before its meldtermijn).
        String caseDocument = caseDocument(kenmerken("late melding", true, false, false, true, false));
        assertNull(play(document(characteristicRule("late melding", compare("later", plusDays(fold("last"), 14), MELDTERMIJN))), caseDocument),
                "plus 14 dg");
        assertNull(play(document(characteristicRule("late melding", compare("later", plusMeldduur(fold("last")), MELDTERMIJN))), caseDocument),
                "plus de meldduur");
        assertNull(play(document(characteristicRule("late melding", compare("earlier", MELDTERMIJN, plusDays(fold("first"), 14)))),
                caseDocument), "rechts, plus 14 dg");
    }

    @Test void against_a_date_parameter() throws Exception {
        // "… de laatste van … later is dan de peildatum", a date parameter (03-04-2026): J1 and J5.
        assertNull(play(document(characteristicRule("late melding", compare("later", fold("last"), AS_OF_DATE))),
                caseDocument(kenmerken("late melding", true, false, false, false, true))));
    }

    @Test void a_member_that_a_later_rule_derives() throws Exception {
        // The fold reads the meldingsdag of a Vondst, which "De meldingsdag van een Vondst moet gesteld worden op zijn
        // bergdag", declared after the reader, writes. A characteristic is written once and stays, so the reader must
        // not decide on the transient in which no meldingsdag is written yet (RUNTIME-SEMANTICS ch. 6.1). "… later is
        // dan zijn meldtermijn": J1. "… leeg is": J2 alone. In a justification, "Uitzondering op meldplicht … geen
        // meldplicht indien de laatste van de meldingsdag … leeg is": J1, J3 and J5.
        String meldingsdag = "{\"node\":\"Rule\",\"name\":\"meldingsdag\",\"versions\":[{\"validity\":{\"node\":"
                + "\"Validity\"},\"target\":" + own("meldingsdag", "Vondst") + ",\"value\":" + own("bergdag", "Vondst")
                + ",\"initialisation\":false,\"presentation\":\"equation\"}]}";
        String later = compare("later", fold("last", "meldingsdag"), MELDTERMIJN);
        assertNull(play(rules(List.of(characteristicRule("late melding", later), meldingsdag)),
                caseDocument(kenmerken("late melding", true, false, false, false, false))), "later is dan");
        String empty = predicate("empty", fold("last", "meldingsdag"));
        assertNull(play(rules(List.of(characteristicRule("late melding", empty), meldingsdag)),
                caseDocument(kenmerken("late melding", false, true, false, false, false))), "leeg is");
        String vinddag = predicate("filled", own("vinddag", "Jutter"));
        String group = group("meldplicht", "Jutter", "characteristic", "meldplicht",
                "{\"node\":\"Default\",\"decision\":false,\"justification\":" + compound("all", 0, vinddag, empty) + "}",
                "{\"node\":\"Default\",\"decision\":true,\"justification\":" + vinddag + "}");
        assertNull(play(rules(List.of(meldingsdag), group),
                caseDocument(kenmerken("meldplicht", true, false, true, false, true))), "in een rechtvaardiging");
    }

    @Test void equal_not_equal_filled_and_empty() throws Exception {
        // "… gelijk is aan zijn meldtermijn": J4 alone (01-04-2026 both). "… ongelijk is aan …": one leeg operand makes
        // it hold (ch. 1.2), so all but J4. "de laatste van … gevuld is" all but J2, "… leeg is" J2 alone.
        assertNull(play(document(characteristicRule("late melding", compare("equal", fold("last"), MELDTERMIJN))),
                caseDocument(kenmerken("late melding", false, false, false, true, false))), "gelijk");
        assertNull(play(document(characteristicRule("late melding", compare("notEqual", fold("last"), MELDTERMIJN))),
                caseDocument(kenmerken("late melding", true, true, true, false, true))), "ongelijk");
        assertNull(play(document(characteristicRule("late melding", predicate("filled", fold("last")))),
                caseDocument(kenmerken("late melding", true, false, true, true, true))), "gevuld");
        assertNull(play(document(characteristicRule("late melding", predicate("empty", fold("last")))),
                caseDocument(kenmerken("late melding", false, true, false, false, false))), "leeg");
    }

    @Test void the_minimum_and_the_maximum_over_dates() throws Exception {
        // "de maximale waarde van de bergdag van alle vondsten van de Jutter later is dan zijn meldtermijn" (J1, as the
        // last), and "de minimale waarde van … eerder is dan zijn meldtermijn" (J1 and J3), which only a hand-written
        // document spells: the frontend refuses a minimum or a maximum over dates, WF-038 admits it.
        assertNull(play(document(characteristicRule("late melding", compare("later", fold("maximum"), MELDTERMIJN))),
                caseDocument(kenmerken("late melding", true, false, false, false, false))), "maximum");
        assertNull(play(document(characteristicRule("late melding", compare("earlier", fold("minimum"), MELDTERMIJN))),
                caseDocument(kenmerken("late melding", true, false, true, false, false))), "minimum");
    }

    @Test void under_every_quantifier() throws Exception {
        // "indien hij aan alle volgende voorwaarden voldoet: zijn vinddag is gevuld; de laatste van … is later dan zijn
        // meldtermijn" (J1); "… ten minste één van …: zijn vinddag is leeg; de laatste van … is later dan …" (J1 and J4);
        // "… geen van de volgende voorwaarden: de laatste van … is later dan …" (all but J1).
        String late = compare("later", fold("last"), MELDTERMIJN);
        assertNull(play(document(characteristicRule("late melding", compound("all", 0, predicate("filled", own("vinddag",
                "Jutter")), late))), caseDocument(kenmerken("late melding", true, false, false, false, false))), "alle");
        assertNull(play(document(characteristicRule("late melding", compound("atLeast", 1, predicate("empty", own("vinddag",
                "Jutter")), late))), caseDocument(kenmerken("late melding", true, false, false, true, false))), "ten minste één");
        assertNull(play(document(characteristicRule("late melding", compound("none", 0, late))),
                caseDocument(kenmerken("late melding", false, true, true, true, true))), "geen van");
    }

    @Test void in_a_rule_that_writes_an_attribute() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op zijn vinddag indien (de laatste van …) plus 14 dg later is
        // dan zijn meldtermijn": J1 10-03-2026; J4 holds but has no vinddag.
        String regel = "{\"node\":\"Rule\",\"name\":\"uitkomst\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + own("uitkomst", "Jutter") + ",\"value\":" + own("vinddag", "Jutter") + ",\"condition\":"
                + compare("later", plusDays(fold("last"), 14), MELDTERMIJN) + ",\"initialisation\":false,"
                + "\"presentation\":\"equation\"}]}";
        assertNull(play(document(regel), caseDocument(outcomes("2026-03-10", null, null, null, null))));
    }

    @Test void two_roles_far() throws Exception {
        // "Een Kustwacht heeft de late post indien de laatste van de bergdag van alle vondsten van alle jutters van de
        // Kustwacht later is dan zijn sluitdag": K1 (20-04-2026); K3 watches nobody, K4 has no sluitdag.
        assertNull(play(document("{\"node\":\"Rule\",\"name\":\"late post\",\"versions\":[{\"validity\":{\"node\":"
                + "\"Validity\"},\"target\":" + own("late post", "Kustwacht") + ",\"value\":" + WAAR + ",\"condition\":"
                + compare("later", kustFold("last"), own("sluitdag", "Kustwacht")) + ",\"initialisation\":false}]}"),
                caseDocument(kustwachten("late post", true, false, false, false))));
    }

    // ---------------- the justification of a default ----------------

    @Test void in_the_justification_of_an_exception() throws Exception {
        // "Regel meldplicht: Een Jutter heeft de meldplicht indien zijn vinddag gevuld is. Uitzondering op meldplicht:
        // late berging: Een Jutter heeft geen meldplicht indien de laatste van … later is dan zijn meldtermijn", the
        // exception carrying the condition of the base; the same with "(de laatste van …) plus 14 dg", and under "ten
        // minste één van …: zijn vinddag is leeg; …". ref and clingo: J2, J3 and J5 each time (J4 has no vinddag).
        String caseDocument = caseDocument(kenmerken("meldplicht", false, true, true, false, true));
        assertNull(play(document(null, meldplicht(compare("later", fold("last"), MELDTERMIJN))), caseDocument), "zonder rekensom");
        assertNull(play(document(null, meldplicht(compare("later", plusDays(fold("last"), 14), MELDTERMIJN))), caseDocument), "plus 14 dg");
        assertNull(play(document(null, meldplicht(compound("atLeast", 1, predicate("empty", own("vinddag", "Jutter")),
                compare("later", fold("last"), MELDTERMIJN)))), caseDocument), "ten minste één");
        assertNull(play(document(null, meldplicht(compare("later", fold("maximum"), MELDTERMIJN))), caseDocument), "maximum");
    }

    @Test void in_the_justification_of_the_base_and_of_a_deviation() throws Exception {
        // The base: "De uitkomst van een Jutter moet gesteld worden op zijn meldtermijn indien de laatste van … later is
        // dan zijn meldtermijn", below "Afwijking van uitkomst: zonder vinddag … gesteld worden op 01-01-2026 indien zijn
        // vinddag leeg is": J1 10-04-2026, J4 01-01-2026, the others leeg. A deviation: "… gesteld worden op zijn
        // meldtermijn indien zijn meldtermijn eerder is dan de laatste van …" above the vinddag: J1 10-04-2026, J2, J3
        // and J5 their vinddag, J4 leeg.
        String base = defaultEntry(compare("later", fold("last"), MELDTERMIJN), MELDTERMIJN);
        String withoutFindDay = defaultEntry(predicate("empty", own("vinddag", "Jutter")), DAY_ONE);
        assertNull(play(document(null, group("uitkomst", "Jutter", "attribute", "uitkomst", withoutFindDay, base)),
                caseDocument(outcomes("2026-04-10", null, null, "2026-01-01", null))), "de basis");
        String late = defaultEntry(compare("earlier", MELDTERMIJN, fold("last")), MELDTERMIJN);
        String vinddag = defaultEntry(predicate("filled", own("vinddag", "Jutter")), own("vinddag", "Jutter"));
        assertNull(play(document(null, group("uitkomst", "Jutter", "attribute", "uitkomst", late, vinddag)),
                caseDocument(outcomes("2026-04-10", "2026-03-10", "2026-03-10", null, "2026-03-10"))), "een afwijking");
    }

    @Test void two_roles_far_in_a_justification() throws Exception {
        // "Regel open post: Een Kustwacht heeft de open post indien zijn sluitdag gevuld is. Uitzondering op open post:
        // late vondst: … geen open post indien de laatste van … van alle jutters van de Kustwacht later is dan zijn
        // sluitdag": K2 and K3 (an empty source); K4 has no sluitdag.
        String open = "{\"node\":\"Default\",\"decision\":true,\"justification\":"
                + predicate("filled", own("sluitdag", "Kustwacht")) + "}";
        String late = "{\"node\":\"Default\",\"decision\":false,\"justification\":" + compound("all", 0,
                predicate("filled", own("sluitdag", "Kustwacht")), compare("later", kustFold("last"), own("sluitdag",
                "Kustwacht"))) + "}";
        assertNull(play(document(null, group("open post", "Kustwacht", "characteristic", "open post", late, open)),
                caseDocument(kustwachten("open post", false, true, true, false))));
    }

    // ---------------- documents ----------------

    private static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String DG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + DG + "}";
    private static final String WAAR = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"}}";
    private static final String DAY_ONE = "{\"node\":\"Literal\",\"value\":{\"date\":\"2026-01-01\"},\"type\":" + DATE + "}";

    private static String own(String attribute, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribute + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static final String MELDTERMIJN = own("meldtermijn", "Jutter");
    private static final String AS_OF_DATE = "{\"node\":\"ParameterReference\",\"parameter\":\"peildatum\"}";

    /** "de &lt;kind&gt; van de bergdag van alle vondsten van de Jutter" (laatste, eerste, maximale or minimale waarde). */
    private static String fold(String kind) {
        return fold(kind, "bergdag");
    }

    private static String fold(String kind, String member) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + own(member, "Vondst")
                + "],\"elementType\":\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\","
                + "\"role\":\"vondst\"},\"type\":" + DATE + "}";
    }

    /** "de &lt;kind&gt; van de bergdag van alle vondsten van alle jutters van de Kustwacht". */
    private static String kustFold(String kind) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + own("bergdag", "Vondst")
                + "],\"elementType\":\"Vondst\",\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":"
                + "\"KustwachtBewaaktJutter\",\"role\":\"jutter\"},{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\","
                + "\"role\":\"vondst\"}],\"type\":" + DATE + "}";
    }

    private static String plusDays(String date, int n) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + date + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"" + n + "\"},\"type\":" + DAGEN + "},\"type\":" + DATE + "}";
    }

    private static String plusMeldduur(String date) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + date + ",\"right\":{\"node\":\"ParameterReference\","
                + "\"parameter\":\"meldduur\"},\"type\":" + DATE + "}";
    }

    private static String compare(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + left + ",\"right\":" + right + "}";
    }

    private static String predicate(String category, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + category + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0}";
    }

    private static String compound(String quantifier, int threshold, String... parts) {
        return "{\"node\":\"Compound\",\"quantifier\":\"" + quantifier + "\",\"threshold\":" + threshold + ",\"parts\":["
                + String.join(",", parts) + "]}";
    }

    /** "Een Jutter heeft de &lt;kenmerk&gt; indien &lt;voorwaarde&gt;". */
    private static String characteristicRule(String characteristic, String condition) {
        return "{\"node\":\"Rule\",\"name\":\"" + characteristic + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + own(characteristic, "Jutter") + ",\"value\":" + WAAR + ",\"condition\":" + condition
                + ",\"initialisation\":false}]}";
    }

    private static String defaultEntry(String justification, String consequence) {
        return "{\"node\":\"Default\",\"justification\":" + justification + ",\"consequence\":" + consequence + "}";
    }

    /** A group on {@code type}; {@code soort} is "attribute" or "characteristic". */
    private static String group(String naam, String type, String category, String target, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"" + category + "\":\""
                + target + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** "Regel meldplicht" with "Uitzondering op meldplicht: late berging … indien &lt;voorwaarde&gt;", which carries the
     *  condition of the base. */
    private static String meldplicht(String condition) {
        String vinddag = predicate("filled", own("vinddag", "Jutter"));
        return group("meldplicht", "Jutter", "characteristic", "meldplicht",
                "{\"node\":\"Default\",\"decision\":false,\"justification\":" + compound("all", 0, vinddag, condition) + "}",
                "{\"node\":\"Default\",\"decision\":true,\"justification\":" + vinddag + "}");
    }

    private static String attribute(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String characteristic(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"possessive\"}";
    }

    private static String role(String naam, String type, String plural) {
        return "{\"node\":\"Role\",\"name\":\"" + naam + "\",\"objectType\":\"" + type + "\""
                + (plural == null ? ",\"many\":false}" : ",\"plural\":\"" + plural + "\",\"many\":true}");
    }

    private static String document(String regel, String... groups) {
        return rules(regel == null ? List.of() : List.of(regel), groups);
    }

    /** Kustwacht, Jutter and Vondst, KustwachtBewaaktJutter and JutterHeeftVondst, the parameters meldduur (dagen) and
     *  peildatum. */
    private static String rules(List<String> rules, String... groups) {
        String model = "{\"objectTypes\":["
                + "{\"node\":\"ObjectType\",\"name\":\"Kustwacht\",\"animate\":false,\"plural\":\"Kustwachten\",\"attributes\":["
                + attribute("sluitdag", DATE) + "],\"characteristics\":[" + characteristic("late post") + "," + characteristic("open post") + "]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
                + attribute("vinddag", DATE) + "," + attribute("meldtermijn", DATE) + "," + attribute("uitkomst", DATE)
                + "],\"characteristics\":[" + characteristic("late melding") + "," + characteristic("meldplicht") + "]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribute("soort", "{\"node\":\"Text\"}") + "," + attribute("bergdag", DATE) + ","
                + attribute("meldingsdag", DATE) + "],\"characteristics\":[]}],"
                + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"KustwachtBewaaktJutter\",\"reciprocal\":false,\"roles\":["
                + role("kustwacht", "Kustwacht", null) + "," + role("jutter", "Jutter", "jutters") + "]},"
                + "{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
                + role("vinder", "Jutter", null) + "," + role("vondst", "Vondst", "vondsten") + "]}],"
                + "\"parameters\":[{\"node\":\"Parameter\",\"name\":\"meldduur\",\"datatype\":" + DAGEN + "},"
                + "{\"node\":\"Parameter\",\"name\":\"peildatum\",\"datatype\":" + DATE + "}],\"domains\":[],"
                + "\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\","
                + "\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]}]}],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groups) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instance(String label, String type, String attributes, String roles) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributes + "},\"roles\":{"
                + roles + "},\"characteristics\":[]}";
    }

    private static String day(String naam, String iso) {
        return iso == null ? null : "\"" + naam + "\":{\"date\":\"" + iso + "\"}";
    }

    private static String attrs(String... parts) {
        List<String> out = new ArrayList<>();
        for (String d : parts) if (d != null) out.add(d);
        return String.join(",", out);
    }

    private static String jutter(String label, String vinddag, String meldtermijn, String kustwacht) {
        return instance(label, "Jutter", attrs(day("vinddag", vinddag), day("meldtermijn", meldtermijn)),
                kustwacht == null ? "" : "\"kustwacht\":[\"" + kustwacht + "\"]");
    }

    private static String vondst(String label, String vinder, String category, String bergdag) {
        return instance(label, "Vondst", attrs("\"soort\":{\"text\":\"" + category + "\"}", day("bergdag", bergdag)),
                "\"vinder\":[\"" + vinder + "\"]");
    }

    private static final List<String> SITUATIE = List.of(
            instance("K1", "Kustwacht", attrs(day("sluitdag", "2026-04-15")), ""),
            instance("K2", "Kustwacht", attrs(day("sluitdag", "2026-04-15")), ""),
            instance("K3", "Kustwacht", attrs(day("sluitdag", "2026-04-15")), ""),
            instance("K4", "Kustwacht", "", ""),
            jutter("J1", "2026-03-10", "2026-04-10", "K1"), jutter("J2", "2026-03-10", "2026-04-10", "K1"),
            jutter("J3", "2026-03-10", "2026-05-10", "K2"), jutter("J4", null, "2026-04-01", null),
            jutter("J5", "2026-03-10", null, "K4"),
            vondst("V1", "J1", "lading", "2026-04-02"), vondst("V2", "J1", "anker", "2026-04-20"),
            vondst("V3", "J3", "lading", "2026-04-01"), vondst("V4", "J3", "anker", null),
            vondst("V5", "J4", "lading", "2026-04-01"), vondst("V6", "J5", "anker", "2026-04-05"));

    /** The characteristic of J1 to J5 in order. */
    private static List<String> kenmerken(String characteristic, boolean... on) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < on.length; i++)
            out.add("{\"kind\":\"characteristic" + (on[i] ? "Present" : "Absent") + "\",\"label\":\"J" + (i + 1)
                    + "\",\"characteristic\":\"" + characteristic + "\"}");
        return out;
    }

    /** The characteristic of K1 to K4 in order. */
    private static List<String> kustwachten(String characteristic, boolean... on) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < on.length; i++)
            out.add("{\"kind\":\"characteristic" + (on[i] ? "Present" : "Absent") + "\",\"label\":\"K" + (i + 1)
                    + "\",\"characteristic\":\"" + characteristic + "\"}");
        return out;
    }

    /** The uitkomst of J1 to J5 in order; null is leeg. */
    private static List<String> outcomes(String... iso) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < iso.length; i++)
            out.add(iso[i] == null ? "{\"kind\":\"attributeEmpty\",\"label\":\"J" + (i + 1) + "\",\"attribute\":\"uitkomst\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"J" + (i + 1) + "\",\"attribute\":\"uitkomst\",\"value\":"
                    + "{\"date\":\"" + iso[i] + "\"}}");
        return out;
    }

    private static String caseDocument(List<String> expected) {
        List<String> all = new ArrayList<>(expected);
        all.add("{\"kind\":\"consistent\"}");
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", SITUATIE)
                + "],\"parameters\":{\"meldduur\":{\"number\":\"14\"},\"peildatum\":{\"date\":\"2026-04-03\"}}},\"expected\":[" + String.join(",", all) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String play(String ir, String caseDocument) throws Exception {
        Path tmp = Files.createTempFile("datumvouw-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.playCase(tmp, JSON.readTree(caseDocument), false);
    }
}
