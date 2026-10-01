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
 * (vinddag 10-03-2026, no meldtermijn) found V6 (anker, 05-04-2026). The meldduur is 14 dagen.
 *
 * <p><b>Red before the fix</b> (measured 2026-10-01 on the transpiler of the claim, exec-model branch): a condition
 * refused the rule ({@code voorwaarde met een ordenende vouw over een datumbron (eerste, laatste, minimum of maximum
 * over een rol), die deze tak alleen als waarde vouwt …}) and a justification the group ({@code rechtvaardiging niet
 * vertaald, een operand die deze tak niet als waarde leest: …}; for a minimum or maximum {@code … een vergelijking van
 * een aggregatie met iets anders dan een getal …}).
 */
class DatumvouwVoorwaardeWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- the condition of a rule ----------------

    @Test void de_laatste_bergdag_links_en_rechts() throws Exception {
        // "Een Jutter heeft de late melding indien de laatste van de bergdag van alle vondsten van de Jutter later is
        // dan zijn meldtermijn", and "indien zijn meldtermijn eerder is dan de laatste van …". ref and clingo: J1 only.
        String zaak = zaak(kenmerken("late melding", true, false, false, false, false));
        assertNull(speel(document(kenmerkRegel("late melding", vgl("later", vouw("last"), MELDTERMIJN))), zaak), "links");
        assertNull(speel(document(kenmerkRegel("late melding", vgl("earlier", MELDTERMIJN, vouw("last")))), zaak), "rechts");
    }

    @Test void met_een_datumrekensom_en_een_parameter() throws Exception {
        // "(de laatste van …) plus 14 dg later is dan zijn meldtermijn", the same with "plus de meldduur", and "zijn
        // meldtermijn eerder is dan (de eerste van …) plus 14 dg". ref and clingo: J1 and J4, the last two J1 and J4
        // as well (J3 counts from 01-04-2026 to 15-04-2026, before its meldtermijn).
        String zaak = zaak(kenmerken("late melding", true, false, false, true, false));
        assertNull(speel(document(kenmerkRegel("late melding", vgl("later", plusDagen(vouw("last"), 14), MELDTERMIJN))), zaak),
                "plus 14 dg");
        assertNull(speel(document(kenmerkRegel("late melding", vgl("later", plusMeldduur(vouw("last")), MELDTERMIJN))), zaak),
                "plus de meldduur");
        assertNull(speel(document(kenmerkRegel("late melding", vgl("earlier", MELDTERMIJN, plusDagen(vouw("first"), 14)))),
                zaak), "rechts, plus 14 dg");
    }

    @Test void gelijk_ongelijk_gevuld_en_leeg() throws Exception {
        // "… gelijk is aan zijn meldtermijn": J4 alone (01-04-2026 both). "… ongelijk is aan …": one leeg operand makes
        // it hold (ch. 1.2), so all but J4. "de laatste van … gevuld is" all but J2, "… leeg is" J2 alone.
        assertNull(speel(document(kenmerkRegel("late melding", vgl("equal", vouw("last"), MELDTERMIJN))),
                zaak(kenmerken("late melding", false, false, false, true, false))), "gelijk");
        assertNull(speel(document(kenmerkRegel("late melding", vgl("notEqual", vouw("last"), MELDTERMIJN))),
                zaak(kenmerken("late melding", true, true, true, false, true))), "ongelijk");
        assertNull(speel(document(kenmerkRegel("late melding", predicaat("filled", vouw("last")))),
                zaak(kenmerken("late melding", true, false, true, true, true))), "gevuld");
        assertNull(speel(document(kenmerkRegel("late melding", predicaat("empty", vouw("last")))),
                zaak(kenmerken("late melding", false, true, false, false, false))), "leeg");
    }

    @Test void het_minimum_en_het_maximum_over_datums() throws Exception {
        // "de maximale waarde van de bergdag van alle vondsten van de Jutter later is dan zijn meldtermijn" (J1, as the
        // last), and "de minimale waarde van … eerder is dan zijn meldtermijn" (J1 and J3), which only a hand-written
        // document spells: the frontend refuses a minimum or a maximum over dates, WF-038 admits it.
        assertNull(speel(document(kenmerkRegel("late melding", vgl("later", vouw("maximum"), MELDTERMIJN))),
                zaak(kenmerken("late melding", true, false, false, false, false))), "maximum");
        assertNull(speel(document(kenmerkRegel("late melding", vgl("earlier", vouw("minimum"), MELDTERMIJN))),
                zaak(kenmerken("late melding", true, false, true, false, false))), "minimum");
    }

    @Test void onder_elke_kwantor() throws Exception {
        // "indien hij aan alle volgende voorwaarden voldoet: zijn vinddag is gevuld; de laatste van … is later dan zijn
        // meldtermijn" (J1); "… ten minste één van …: zijn vinddag is leeg; de laatste van … is later dan …" (J1 and J4);
        // "… geen van de volgende voorwaarden: de laatste van … is later dan …" (all but J1).
        String laat = vgl("later", vouw("last"), MELDTERMIJN);
        assertNull(speel(document(kenmerkRegel("late melding", samengesteld("all", 0, predicaat("filled", eigen("vinddag",
                "Jutter")), laat))), zaak(kenmerken("late melding", true, false, false, false, false))), "alle");
        assertNull(speel(document(kenmerkRegel("late melding", samengesteld("atLeast", 1, predicaat("empty", eigen("vinddag",
                "Jutter")), laat))), zaak(kenmerken("late melding", true, false, false, true, false))), "ten minste één");
        assertNull(speel(document(kenmerkRegel("late melding", samengesteld("none", 0, laat))),
                zaak(kenmerken("late melding", false, true, true, true, true))), "geen van");
    }

    @Test void in_een_regel_die_een_attribuut_schrijft() throws Exception {
        // "De uitkomst van een Jutter moet gesteld worden op zijn vinddag indien (de laatste van …) plus 14 dg later is
        // dan zijn meldtermijn": J1 10-03-2026; J4 holds but has no vinddag.
        String regel = "{\"node\":\"Rule\",\"name\":\"uitkomst\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + eigen("uitkomst", "Jutter") + ",\"value\":" + eigen("vinddag", "Jutter") + ",\"condition\":"
                + vgl("later", plusDagen(vouw("last"), 14), MELDTERMIJN) + ",\"initialisation\":false,"
                + "\"presentation\":\"equation\"}]}";
        assertNull(speel(document(regel), zaak(uitkomsten("2026-03-10", null, null, null, null))));
    }

    @Test void twee_rollen_ver() throws Exception {
        // "Een Kustwacht heeft de late post indien de laatste van de bergdag van alle vondsten van alle jutters van de
        // Kustwacht later is dan zijn sluitdag": K1 (20-04-2026); K3 watches nobody, K4 has no sluitdag.
        assertNull(speel(document("{\"node\":\"Rule\",\"name\":\"late post\",\"versions\":[{\"validity\":{\"node\":"
                + "\"Validity\"},\"target\":" + eigen("late post", "Kustwacht") + ",\"value\":" + WAAR + ",\"condition\":"
                + vgl("later", kustvouw("last"), eigen("sluitdag", "Kustwacht")) + ",\"initialisation\":false}]}"),
                zaak(kustwachten("late post", true, false, false, false))));
    }

    // ---------------- the justification of a default ----------------

    @Test void in_de_rechtvaardiging_van_een_uitzondering() throws Exception {
        // "Regel meldplicht: Een Jutter heeft de meldplicht indien zijn vinddag gevuld is. Uitzondering op meldplicht:
        // late berging: Een Jutter heeft geen meldplicht indien de laatste van … later is dan zijn meldtermijn", the
        // exception carrying the condition of the base; the same with "(de laatste van …) plus 14 dg", and under "ten
        // minste één van …: zijn vinddag is leeg; …". ref and clingo: J2, J3 and J5 each time (J4 has no vinddag).
        String zaak = zaak(kenmerken("meldplicht", false, true, true, false, true));
        assertNull(speel(document(null, meldplicht(vgl("later", vouw("last"), MELDTERMIJN))), zaak), "zonder rekensom");
        assertNull(speel(document(null, meldplicht(vgl("later", plusDagen(vouw("last"), 14), MELDTERMIJN))), zaak), "plus 14 dg");
        assertNull(speel(document(null, meldplicht(samengesteld("atLeast", 1, predicaat("empty", eigen("vinddag", "Jutter")),
                vgl("later", vouw("last"), MELDTERMIJN)))), zaak), "ten minste één");
        assertNull(speel(document(null, meldplicht(vgl("later", vouw("maximum"), MELDTERMIJN))), zaak), "maximum");
    }

    @Test void in_de_rechtvaardiging_van_de_basis_en_van_een_afwijking() throws Exception {
        // The base: "De uitkomst van een Jutter moet gesteld worden op zijn meldtermijn indien de laatste van … later is
        // dan zijn meldtermijn", below "Afwijking van uitkomst: zonder vinddag … gesteld worden op 01-01-2026 indien zijn
        // vinddag leeg is": J1 10-04-2026, J4 01-01-2026, the others leeg. A deviation: "… gesteld worden op zijn
        // meldtermijn indien zijn meldtermijn eerder is dan de laatste van …" above the vinddag: J1 10-04-2026, J2, J3
        // and J5 their vinddag, J4 leeg.
        String basis = standaard(vgl("later", vouw("last"), MELDTERMIJN), MELDTERMIJN);
        String zonderVinddag = standaard(predicaat("empty", eigen("vinddag", "Jutter")), DAG_EEN);
        assertNull(speel(document(null, groep("uitkomst", "Jutter", "attribute", "uitkomst", zonderVinddag, basis)),
                zaak(uitkomsten("2026-04-10", null, null, "2026-01-01", null))), "de basis");
        String laat = standaard(vgl("earlier", MELDTERMIJN, vouw("last")), MELDTERMIJN);
        String vinddag = standaard(predicaat("filled", eigen("vinddag", "Jutter")), eigen("vinddag", "Jutter"));
        assertNull(speel(document(null, groep("uitkomst", "Jutter", "attribute", "uitkomst", laat, vinddag)),
                zaak(uitkomsten("2026-04-10", "2026-03-10", "2026-03-10", null, "2026-03-10"))), "een afwijking");
    }

    @Test void twee_rollen_ver_in_een_rechtvaardiging() throws Exception {
        // "Regel open post: Een Kustwacht heeft de open post indien zijn sluitdag gevuld is. Uitzondering op open post:
        // late vondst: … geen open post indien de laatste van … van alle jutters van de Kustwacht later is dan zijn
        // sluitdag": K2 and K3 (an empty source); K4 has no sluitdag.
        String open = "{\"node\":\"Default\",\"decision\":true,\"justification\":"
                + predicaat("filled", eigen("sluitdag", "Kustwacht")) + "}";
        String laat = "{\"node\":\"Default\",\"decision\":false,\"justification\":" + samengesteld("all", 0,
                predicaat("filled", eigen("sluitdag", "Kustwacht")), vgl("later", kustvouw("last"), eigen("sluitdag",
                "Kustwacht"))) + "}";
        assertNull(speel(document(null, groep("open post", "Kustwacht", "characteristic", "open post", laat, open)),
                zaak(kustwachten("open post", false, true, true, false))));
    }

    // ---------------- documents ----------------

    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String DG = "{\"node\":\"Unit\",\"abbreviation\":\"dg\",\"system\":\"Tijd\",\"canonical\":\"dg\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1}}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + DG + "}";
    private static final String WAAR = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"}}";
    private static final String DAG_EEN = "{\"node\":\"Literal\",\"value\":{\"date\":\"2026-01-01\"},\"type\":" + DATUM + "}";

    private static String eigen(String attribuut, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static final String MELDTERMIJN = eigen("meldtermijn", "Jutter");

    /** "de &lt;kind&gt; van de bergdag van alle vondsten van de Jutter" (laatste, eerste, maximale or minimale waarde). */
    private static String vouw(String kind) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + eigen("bergdag", "Vondst")
                + "],\"elementType\":\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\","
                + "\"role\":\"vondst\"},\"type\":" + DATUM + "}";
    }

    /** "de &lt;kind&gt; van de bergdag van alle vondsten van alle jutters van de Kustwacht". */
    private static String kustvouw(String kind) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + eigen("bergdag", "Vondst")
                + "],\"elementType\":\"Vondst\",\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":"
                + "\"KustwachtBewaaktJutter\",\"role\":\"jutter\"},{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\","
                + "\"role\":\"vondst\"}],\"type\":" + DATUM + "}";
    }

    private static String plusDagen(String datum, int n) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + datum + ",\"right\":{\"node\":\"Literal\",\"value\":"
                + "{\"number\":\"" + n + "\"},\"type\":" + DAGEN + "},\"type\":" + DATUM + "}";
    }

    private static String plusMeldduur(String datum) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + datum + ",\"right\":{\"node\":\"ParameterReference\","
                + "\"parameter\":\"meldduur\"},\"type\":" + DATUM + "}";
    }

    private static String vgl(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + "}";
    }

    private static String predicaat(String soort, String operand) {
        return "{\"node\":\"Predicate\",\"kind\":\"" + soort + "\",\"operand\":" + operand + ",\"negated\":false,\"digits\":0}";
    }

    private static String samengesteld(String kwantor, int drempel, String... delen) {
        return "{\"node\":\"Compound\",\"quantifier\":\"" + kwantor + "\",\"threshold\":" + drempel + ",\"parts\":["
                + String.join(",", delen) + "]}";
    }

    /** "Een Jutter heeft de &lt;kenmerk&gt; indien &lt;voorwaarde&gt;". */
    private static String kenmerkRegel(String kenmerk, String voorwaarde) {
        return "{\"node\":\"Rule\",\"name\":\"" + kenmerk + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},"
                + "\"target\":" + eigen(kenmerk, "Jutter") + ",\"value\":" + WAAR + ",\"condition\":" + voorwaarde
                + ",\"initialisation\":false}]}";
    }

    private static String standaard(String rechtvaardiging, String gevolg) {
        return "{\"node\":\"Default\",\"justification\":" + rechtvaardiging + ",\"consequence\":" + gevolg + "}";
    }

    /** A group on {@code type}; {@code soort} is "attribute" or "characteristic". */
    private static String groep(String naam, String type, String soort, String doel, String... defaults) {
        return "{\"node\":\"PrioritizedDefaults\",\"name\":\"" + naam + "\",\"subject\":\"" + type + "\",\"" + soort + "\":\""
                + doel + "\",\"defaults\":[" + String.join(",", defaults) + "],\"validity\":{\"node\":\"Validity\"}}";
    }

    /** "Regel meldplicht" with "Uitzondering op meldplicht: late berging … indien &lt;voorwaarde&gt;", which carries the
     *  condition of the base. */
    private static String meldplicht(String voorwaarde) {
        String vinddag = predicaat("filled", eigen("vinddag", "Jutter"));
        return groep("meldplicht", "Jutter", "characteristic", "meldplicht",
                "{\"node\":\"Default\",\"decision\":false,\"justification\":" + samengesteld("all", 0, vinddag, voorwaarde) + "}",
                "{\"node\":\"Default\",\"decision\":true,\"justification\":" + vinddag + "}");
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String kenmerk(String naam) {
        return "{\"node\":\"Characteristic\",\"name\":\"" + naam + "\",\"kind\":\"possessive\"}";
    }

    private static String rol(String naam, String type, String meervoud) {
        return "{\"node\":\"Role\",\"name\":\"" + naam + "\",\"objectType\":\"" + type + "\""
                + (meervoud == null ? ",\"many\":false}" : ",\"plural\":\"" + meervoud + "\",\"many\":true}");
    }

    /** Kustwacht, Jutter and Vondst, KustwachtBewaaktJutter and JutterHeeftVondst, the parameter meldduur (dagen). */
    private static String document(String regel, String... groepen) {
        String model = "{\"objectTypes\":["
                + "{\"node\":\"ObjectType\",\"name\":\"Kustwacht\",\"animate\":false,\"plural\":\"Kustwachten\",\"attributes\":["
                + attribuut("sluitdag", DATUM) + "],\"characteristics\":[" + kenmerk("late post") + "," + kenmerk("open post") + "]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\",\"attributes\":["
                + attribuut("vinddag", DATUM) + "," + attribuut("meldtermijn", DATUM) + "," + attribuut("uitkomst", DATUM)
                + "],\"characteristics\":[" + kenmerk("late melding") + "," + kenmerk("meldplicht") + "]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\",\"attributes\":["
                + attribuut("soort", "{\"node\":\"Text\"}") + "," + attribuut("bergdag", DATUM) + "],\"characteristics\":[]}],"
                + "\"factTypes\":[{\"node\":\"FactType\",\"name\":\"KustwachtBewaaktJutter\",\"reciprocal\":false,\"roles\":["
                + rol("kustwacht", "Kustwacht", null) + "," + rol("jutter", "Jutter", "jutters") + "]},"
                + "{\"node\":\"FactType\",\"name\":\"JutterHeeftVondst\",\"reciprocal\":false,\"roles\":["
                + rol("vinder", "Jutter", null) + "," + rol("vondst", "Vondst", "vondsten") + "]}],"
                + "\"parameters\":[{\"node\":\"Parameter\",\"name\":\"meldduur\",\"datatype\":" + DAGEN + "}],\"domains\":[],"
                + "\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"Tijd\",\"units\":[{\"node\":\"Unit\",\"abbreviation\":\"dg\","
                + "\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"dag\"]}]}],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + model + ",\"rules\":{\"rules\":[" + (regel == null ? "" : regel)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":["
                + String.join(",", groepen) + "]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    private static String instantie(String label, String type, String attributen, String rollen) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[]}";
    }

    private static String dag(String naam, String iso) {
        return iso == null ? null : "\"" + naam + "\":{\"date\":\"" + iso + "\"}";
    }

    private static String attrs(String... delen) {
        List<String> uit = new ArrayList<>();
        for (String d : delen) if (d != null) uit.add(d);
        return String.join(",", uit);
    }

    private static String jutter(String label, String vinddag, String meldtermijn, String kustwacht) {
        return instantie(label, "Jutter", attrs(dag("vinddag", vinddag), dag("meldtermijn", meldtermijn)),
                kustwacht == null ? "" : "\"kustwacht\":[\"" + kustwacht + "\"]");
    }

    private static String vondst(String label, String vinder, String soort, String bergdag) {
        return instantie(label, "Vondst", attrs("\"soort\":{\"text\":\"" + soort + "\"}", dag("bergdag", bergdag)),
                "\"vinder\":[\"" + vinder + "\"]");
    }

    private static final List<String> SITUATIE = List.of(
            instantie("K1", "Kustwacht", attrs(dag("sluitdag", "2026-04-15")), ""),
            instantie("K2", "Kustwacht", attrs(dag("sluitdag", "2026-04-15")), ""),
            instantie("K3", "Kustwacht", attrs(dag("sluitdag", "2026-04-15")), ""),
            instantie("K4", "Kustwacht", "", ""),
            jutter("J1", "2026-03-10", "2026-04-10", "K1"), jutter("J2", "2026-03-10", "2026-04-10", "K1"),
            jutter("J3", "2026-03-10", "2026-05-10", "K2"), jutter("J4", null, "2026-04-01", null),
            jutter("J5", "2026-03-10", null, "K4"),
            vondst("V1", "J1", "lading", "2026-04-02"), vondst("V2", "J1", "anker", "2026-04-20"),
            vondst("V3", "J3", "lading", "2026-04-01"), vondst("V4", "J3", "anker", null),
            vondst("V5", "J4", "lading", "2026-04-01"), vondst("V6", "J5", "anker", "2026-04-05"));

    /** The characteristic of J1 to J5 in order. */
    private static List<String> kenmerken(String kenmerk, boolean... aan) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < aan.length; i++)
            uit.add("{\"kind\":\"characteristic" + (aan[i] ? "Present" : "Absent") + "\",\"label\":\"J" + (i + 1)
                    + "\",\"characteristic\":\"" + kenmerk + "\"}");
        return uit;
    }

    /** The characteristic of K1 to K4 in order. */
    private static List<String> kustwachten(String kenmerk, boolean... aan) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < aan.length; i++)
            uit.add("{\"kind\":\"characteristic" + (aan[i] ? "Present" : "Absent") + "\",\"label\":\"K" + (i + 1)
                    + "\",\"characteristic\":\"" + kenmerk + "\"}");
        return uit;
    }

    /** The uitkomst of J1 to J5 in order; null is leeg. */
    private static List<String> uitkomsten(String... iso) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < iso.length; i++)
            uit.add(iso[i] == null ? "{\"kind\":\"attributeEmpty\",\"label\":\"J" + (i + 1) + "\",\"attribute\":\"uitkomst\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"J" + (i + 1) + "\",\"attribute\":\"uitkomst\",\"value\":"
                    + "{\"date\":\"" + iso[i] + "\"}}");
        return uit;
    }

    private static String zaak(List<String> verwacht) {
        List<String> alle = new ArrayList<>(verwacht);
        alle.add("{\"kind\":\"consistent\"}");
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", SITUATIE)
                + "],\"parameters\":{\"meldduur\":{\"number\":\"14\"}}},\"expected\":[" + String.join(",", alle) + "]}";
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        Path tmp = Files.createTempFile("datumvouw-voorwaarde", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return CaseRunner.speelCase(tmp, JSON.readTree(zaak), false);
    }
}
