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
 * A fold over the members of a set source inside a rule's value, in the exec-model branch: the earliest and the
 * latest date over a to-many role ("de eerste van de vinddag van alle vondsten van de Jutter"), with a condition on
 * the elements, two roles deep, and as an operand of a computation, beside the number folds as an operand.
 * RUNTIME-SEMANTICS ch. 1.2e: eerste and laatste are ordering folds over the contributed values, empty contributors
 * are skipped and an empty or all-empty source is leeg; ch. 1.2 gives the empty operand of each operator. Every
 * document here is authored in this class on Jutterswet material, and each is one measured form of the card's form
 * space, with the values {@code ref} and clingo answer (clingo does not fold a date-and-time over a set; there
 * {@code ref} alone).
 *
 * <p><b>Red before the fix</b> (measured 2026-09-30 on the jar of the claim, exec-model branch): every date fold
 * refused {@code regel (waarde buiten scope)}; a date plus a number fold, a clamp around a count and a count plus a
 * navigated read refused {@code regel (aggregatie-in-arithmetiek buiten scope)}; the minimum and the maximum of a
 * number plus 1 answered leeg over an empty source where ref and clingo answer 1 ({@code verwacht 1, werkelijk
 * leeg}), and a percentage of a sum answered leeg for every subject ({@code verwacht 1.90, werkelijk leeg}); a date
 * fold in a condition refused {@code voorwaarde buiten Phase-B scope}, which names no form. One row is a flank,
 * green before and after: the number folds the branch already played.
 */
class DatumvouwOverRolWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test void de_eerste_en_de_laatste_datum_over_een_rol() throws Exception {
        // The card's reproduction and its twin: "De eerste vinddag van een Jutter moet berekend worden als de eerste
        // van de vinddag van alle vondsten van de Jutter", and "de laatste van". J1's first member is neither the
        // earliest nor the latest date and its last filled member is the earliest, so a positional reading fails
        // both; J2 has no vondsten, J3 one without vinddag, J4 one. ref and clingo: J1 05-03-2026 and 20-03-2026.
        String ir = document(regel("eerste vinddag", "Jutter", "eerste vinddag", vouw("first", "vinddag", null)),
                regel("laatste vinddag", "Jutter", "laatste vinddag", vouw("last", "vinddag", null)));
        assertNull(speel(ir, zaak(situatie(true, false, null),
                        perJutter("eerste vinddag", datum("2026-03-05"), null, null, datum("2025-12-31")),
                        perJutter("laatste vinddag", datum("2026-03-20"), null, null, datum("2025-12-31")))),
                "eerste en laatste over de vondsten");
    }

    @Test void gesteld_en_geinitialiseerd() throws Exception {
        // "moet gesteld worden op de eerste van …", and "moet geïnitialiseerd worden op de eerste van …" where J4
        // carries its own eerste vinddag, which the initialisation leaves standing. ref and clingo: J4 01-01-2020.
        String gelijk = document(gesteld("eerste vinddag", "Jutter", "eerste vinddag", vouw("first", "vinddag", null)));
        assertNull(speel(gelijk, zaak(situatie(true, false, null),
                        perJutter("eerste vinddag", datum("2026-03-05"), null, null, datum("2025-12-31")))),
                "gesteld");
        String init = document(initialisatie("eerste vinddag", "Jutter", "eerste vinddag", vouw("first", "vinddag", null)));
        assertNull(speel(init, zaak(situatie(true, false, "2020-01-01"),
                        perJutter("eerste vinddag", datum("2026-03-05"), null, null, datum("2020-01-01")))),
                "geïnitialiseerd");
    }

    @Test void een_voorwaarde_op_de_elementen() throws Exception {
        // "… van alle vondsten van de Jutter die zwaar zijn", zwaar derived by a rule declared after the fold (a Vondst
        // is zwaar when its gewicht is above 5: V2, V3 and V6), and "… die later zijn dan 10-03-2026", a test on the
        // folded date itself. ref and clingo: J1 20-03-2026 and 12-03-2026; J4 31-12-2025 and leeg.
        String zwaar = document(regel("eerste vinddag", "Jutter", "eerste vinddag", vouw("first", "vinddag", KENMERK_ZWAAR)),
                kenmerkRegel("zwaar", "Vondst", vergelijking("greater", eigen("gewicht", "Vondst"), getalLiteraal("5"))));
        assertNull(speel(zwaar, zaak(situatie(false, false, null),
                        perJutter("eerste vinddag", datum("2026-03-20"), null, null, datum("2025-12-31")))),
                "een later afgeleid kenmerk als voorwaarde");
        String later = document(regel("eerste vinddag", "Jutter", "eerste vinddag",
                vouw("first", "vinddag", vergelijking("later", eigen("vinddag", "Vondst"), datumLiteraal("2026-03-10")))));
        assertNull(speel(later, zaak(situatie(true, false, null),
                        perJutter("eerste vinddag", datum("2026-03-12"), null, null, null))),
                "een toets op de gevouwen datum zelf");
    }

    @Test void een_datum_die_later_wordt_afgeleid() throws Exception {
        // "De laatste bergdag van een Jutter moet berekend worden als de laatste van de bergdag van alle vondsten van
        // de Jutter", declared before "De bergdag van een Vondst moet berekend worden als de vinddag van de Vondst plus
        // 1 dag". ref and clingo: J1 21-03-2026, J4 01-01-2026.
        String ir = document(regel("laatste bergdag", "Jutter", "laatste bergdag", vouw("last", "bergdag", null)),
                regel("bergdag", "Vondst", "bergdag", binair("plus", eigen("vinddag", "Vondst"), duur("1"), DATUM)));
        assertNull(speel(ir, zaak(situatie(true, false, null),
                        perJutter("laatste bergdag", datum("2026-03-21"), null, null, datum("2026-01-01")))),
                "een vouw over een later afgeleide datum");
    }

    @Test void de_datumvouw_plus_en_min_een_duur() throws Exception {
        // "(de eerste van de vinddag van alle vondsten van de Jutter) plus 30 dagen" and "(de laatste van …) min 7
        // dagen"; an empty date propagates, so J2 and J3 stay leeg. ref and clingo: J1 04-04-2026 and 13-03-2026.
        String plus = document(regel("meldgrens", "Jutter", "meldgrens",
                binair("plus", vouw("first", "vinddag", null), duur("30"), DATUM)));
        assertNull(speel(plus, zaak(situatie(true, false, null),
                        perJutter("meldgrens", datum("2026-04-04"), null, null, datum("2026-01-30")))),
                "de eerste plus 30 dagen");
        String min = document(regel("meldgrens", "Jutter", "meldgrens",
                binair("minus", vouw("last", "vinddag", null), duur("7"), DATUM)));
        assertNull(speel(min, zaak(situatie(true, false, null),
                        perJutter("meldgrens", datum("2026-03-13"), null, null, datum("2025-12-24")))),
                "de laatste min 7 dagen");
    }

    @Test void een_datum_plus_een_getalvouw_als_duur() throws Exception {
        // "De meldgrens van een Jutter moet berekend worden als zijn startdag plus de maximale waarde van de zoekduur
        // van alle vondsten van de Jutter", the zoekduur in dagen; an empty duration coalesces to 0, an empty date
        // propagates. ref and clingo: J1 21-03-2026, J2 01-03-2026, J3 leeg, J4 06-01-2026.
        String ir = document(regel("meldgrens", "Jutter", "meldgrens",
                binair("plus", eigen("startdag", "Jutter"), vouw("maximum", "zoekduur", null), DATUM)));
        assertNull(speel(ir, zaak(situatie(true, false, null),
                        perJutter("meldgrens", datum("2026-03-21"), datum("2026-03-01"), null, datum("2026-01-06")))),
                "een datum plus het maximum van een duur");
    }

    @Test void de_tijdsduur_tussen_de_eerste_en_de_laatste() throws Exception {
        // "de tijdsduur van de eerste van … tot de laatste van … in hele dagen"; an empty operand propagates. ref and
        // clingo: J1 15, J4 0.
        String ir = document(regel("spanne", "Jutter", "spanne", "{\"node\":\"BuiltinCall\",\"kind\":\"duration\",\"arguments\":["
                + vouw("first", "vinddag", null) + "," + vouw("last", "vinddag", null) + "],\"type\":" + DAGEN + "}"));
        assertNull(speel(ir, zaak(situatie(true, false, null),
                        perJutter("spanne", getal("15"), null, null, getal("0")))),
                "de tijdsduur tussen twee datumvouwen");
    }

    @Test void twee_rollen_diep() throws Exception {
        // "de eerste van de vinddag van alle vondsten van alle beheerde jutters van de Strandvoogd": S1 beheert J1 and
        // J3, S2 beheert J2, S3 beheert J4, S4 niemand. ref and clingo: S1 05-03-2026 and 20-03-2026, S3 31-12-2025.
        String ir = document(regel("eerste vinddag", "Strandvoogd", "eerste vinddag", padvouw("first", "vinddag")),
                regel("laatste vinddag", "Strandvoogd", "laatste vinddag", padvouw("last", "vinddag")));
        List<String> verwacht = new ArrayList<>();
        verwacht.addAll(perVoogd("eerste vinddag", datum("2026-03-05"), null, datum("2025-12-31"), null));
        verwacht.addAll(perVoogd("laatste vinddag", datum("2026-03-20"), null, datum("2025-12-31"), null));
        assertNull(speel(ir, zaak(situatie(true, false, null), verwacht)), "de vouw over een pad van twee rollen");
    }

    @Test void een_datum_en_tijd() throws Exception {
        // "Het eerste vindtijdstip van een Jutter moet berekend worden als de eerste van het vindtijdstip van alle
        // vondsten van de Jutter". ref: J1 05-03-2026 23:59:59.999, J4 31-12-2025 00:00:00.001 (clingo does not fold a
        // date-and-time over a set).
        String ir = document(regel("eerste vindtijdstip", "Jutter", "eerste vindtijdstip", vouw("first", "vindtijdstip", null)));
        assertNull(speel(ir, zaak(situatie(true, true, null),
                        perJutter("eerste vindtijdstip", tijdstip("2026-03-05T23:59:59.999"), null, null,
                                tijdstip("2025-12-31T00:00:00.001")))),
                "de eerste van een datum-en-tijd");
    }

    @Test void een_getalvouw_als_operand() throws Exception {
        // ch. 1.2: plus coalesces an empty operand, so the minimum or maximum of an empty source plus 1 is 1; a
        // percentage of an empty sum is 0. ref and clingo answer each value below.
        String min = document(regel("telling", "Jutter", "telling", binair("plus", vouw("minimum", "gewicht", null), getalLiteraal("1"), GEHEEL)));
        assertNull(speel(min, zaak(situatie(true, false, null), perJutter("telling", getal("3"), getal("1"), getal("1"), getal("10")))),
                "het minimum plus 1");
        String max = document(regel("telling", "Jutter", "telling", binair("plus", vouw("maximum", "gewicht", null), getalLiteraal("1"), GEHEEL)));
        assertNull(speel(max, zaak(situatie(true, false, null), perJutter("telling", getal("8"), getal("1"), getal("1"), getal("10")))),
                "het maximum plus 1");
        String pct = document(regel("uitkomst", "Jutter", "uitkomst", binair("percentageOf", getalLiteraal("10"),
                vouw("sum", "gewicht", null), "{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}")));
        assertNull(speel(pct, zaak(situatie(true, false, null), perJutter("uitkomst", getal("1.90"), getal("0.00"), getal("0.00"), getal("0.90")))),
                "een percentage van een som");
        String begrensd = document(regel("telling", "Jutter", "telling", "{\"node\":\"Clamp\",\"base\":"
                + binair("minus", telling(), getalLiteraal("2"), GEHEEL) + ",\"minimum\":" + getalLiteraal("0") + ",\"type\":" + GEHEEL + "}"));
        assertNull(speel(begrensd, zaak(situatie(true, false, null), perJutter("telling", getal("2"), getal("0"), getal("0"), getal("0")))),
                "een begrenzing om een telling");
        String nav = document(regel("telling", "Jutter", "telling", binair("plus", telling(),
                "{\"node\":\"AttributeReference\",\"attribute\":\"bonus\",\"subjectType\":\"Strandvoogd\",\"path\":[{\"node\":\"RoleBinding\","
                        + "\"factType\":\"StrandvoogdBeheertJutter\",\"role\":\"strandvoogd\"}]}", GEHEEL)));
        assertNull(speel(nav, zaak(situatie(true, false, null), perJutter("telling", getal("104"), getal("0"), getal("101"), getal("1")))),
                "een telling plus een genavigeerde lezing");
    }

    @Test void de_getalvouwen_die_al_speelden_spelen_gelijk() throws Exception {
        // A flank, green before and after: the count plus 1 (the card's k1b), the sum plus 1, the sum reduced by 1.
        String aantal = document(regel("telling", "Jutter", "telling", binair("plus", telling(), getalLiteraal("1"), GEHEEL)));
        assertNull(speel(aantal, zaak(situatie(true, false, null), perJutter("telling", getal("5"), getal("1"), getal("2"), getal("2")))),
                "het aantal plus 1");
        String som = document(regel("telling", "Jutter", "telling", binair("plus", vouw("sum", "gewicht", null), getalLiteraal("1"), GEHEEL)));
        assertNull(speel(som, zaak(situatie(true, false, null), perJutter("telling", getal("20"), getal("1"), getal("1"), getal("10")))),
                "de som plus 1");
        String vm = document(regel("telling", "Jutter", "telling", binair("reducedBy", vouw("sum", "gewicht", null), getalLiteraal("1"), GEHEEL)));
        assertNull(speel(vm, zaak(situatie(true, false, null), perJutter("telling", getal("18"), null, null, getal("8")))),
                "de som verminderd met 1");
    }

    @Test void een_datumvouw_in_een_voorwaarde_weigert_met_de_vorm() throws Exception {
        // "Een Jutter is vroeg indien de eerste van de vinddag van alle vondsten van de Jutter eerder is dan
        // 01-03-2026": ref and clingo answer J4 vroeg. This branch folds a date only in a rule's value, and the refusal
        // must name that form.
        String ir = document(kenmerkRegel("vroeg", "Jutter",
                vergelijking("earlier", vouw("first", "vinddag", null), datumLiteraal("2026-03-01"))));
        Path p = tijdelijk(ir);
        JsonNode c = JSON.readTree(zaak(situatie(true, false, null), List.of()));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(p, c, false));
        assertTrue(e.getMessage().contains("een datumvouw (EERSTE/LAATSTE) over een rol"), e.getMessage());
    }

    // ---------------- documents ----------------

    private static final String GEHEEL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
    private static final String DAGEN = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":"
            + "{\"node\":\"Unit\",\"abbreviation\":\"dag\",\"canonical\":\"dag\",\"factorToCanonical\":{\"num\":1,\"den\":1}}}";
    private static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String TIJDSTIP = "{\"node\":\"Date\",\"precision\":\"millisecond\"}";
    private static final String KENMERK_ZWAAR = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"zwaar\",\"negated\":false}";

    /** Strandvoogd, Jutter and Vondst; one jutter heeft meerdere vondsten, one strandvoogd beheert meerdere beheerde
     *  jutters. */
    private static final String MODEL = "{\"objectTypes\":["
            + objecttype("Strandvoogd", "Strandvoogden", true, List.of(attr("eerste vinddag", DATUM), attr("laatste vinddag", DATUM),
                    attr("bonus", GEHEEL)), "")
            + "," + objecttype("Jutter", "Jutters", true, List.of(attr("eerste vinddag", DATUM), attr("laatste vinddag", DATUM),
                    attr("laatste bergdag", DATUM), attr("meldgrens", DATUM), attr("startdag", DATUM), attr("telling", GEHEEL),
                    attr("uitkomst", "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}"),
                    attr("spanne", DAGEN), attr("eerste vindtijdstip", TIJDSTIP)),
                    "{\"node\":\"Characteristic\",\"name\":\"vroeg\",\"kind\":\"adjectival\"}")
            + "," + objecttype("Vondst", "Vondsten", false, List.of(attr("vinddag", DATUM), attr("bergdag", DATUM),
                    attr("vindtijdstip", TIJDSTIP), attr("gewicht", GEHEEL), attr("zoekduur", DAGEN)),
                    "{\"node\":\"Characteristic\",\"name\":\"zwaar\",\"kind\":\"adjectival\"}")
            + "],\"factTypes\":["
            + feittype("JutterHeeftVondst", "jutter", "Jutter", "vondst", "vondsten", "Vondst") + ","
            + feittype("StrandvoogdBeheertJutter", "strandvoogd", "Strandvoogd", "beheerde jutter", "beheerde jutters", "Jutter")
            + "],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]}";

    private static String objecttype(String naam, String meervoud, boolean bezield, List<String> attributen, String kenmerk) {
        return "{\"node\":\"ObjectType\",\"name\":\"" + naam + "\",\"animate\":" + bezield + ",\"plural\":\"" + meervoud
                + "\",\"attributes\":[" + String.join(",", attributen) + "],\"characteristics\":[" + kenmerk + "]}";
    }

    private static String attr(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"" + naam + "\",\"datatype\":" + datatype + "}";
    }

    private static String feittype(String naam, String eenRol, String eenType, String veelRol, String veelMeervoud, String veelType) {
        return "{\"node\":\"FactType\",\"name\":\"" + naam + "\",\"reciprocal\":false,\"roles\":["
                + "{\"node\":\"Role\",\"name\":\"" + eenRol + "\",\"objectType\":\"" + eenType + "\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"" + veelRol + "\",\"objectType\":\"" + veelType + "\",\"plural\":\"" + veelMeervoud
                + "\",\"many\":true}]}";
    }

    private static String eigen(String naam, String type) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + naam + "\",\"subjectType\":\"" + type + "\"}";
    }

    private static String getalLiteraal(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + GEHEEL + "}";
    }

    private static String duur(String n) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"" + n + "\"},\"type\":" + DAGEN + "}";
    }

    private static String datumLiteraal(String iso) {
        return "{\"node\":\"Literal\",\"value\":{\"date\":\"" + iso + "\"},\"type\":" + DATUM + "}";
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binary\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + type + "}";
    }

    private static String vergelijking(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"" + op + "\",\"left\":" + links + ",\"right\":" + rechts + "}";
    }

    /** A fold of {@code kind} over {@code lid} of the vondsten of the Jutter; {@code filter} null for none. */
    private static String vouw(String kind, String lid, String filter) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + eigen(lid, "Vondst") + "],\"elementType\":"
                + "\"Vondst\"" + (filter == null ? "" : ",\"filter\":" + filter) + ",\"sourceRole\":{\"node\":\"RoleBinding\","
                + "\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"},\"type\":" + vouwType(kind, lid) + "}";
    }

    /** A fold of {@code kind} over {@code lid} of the vondsten of all beheerde jutters of the Strandvoogd. */
    private static String padvouw(String kind, String lid) {
        return "{\"node\":\"Aggregation\",\"kind\":\"" + kind + "\",\"arguments\":[" + eigen(lid, "Vondst") + "],\"elementType\":"
                + "\"Vondst\",\"sourcePath\":[{\"node\":\"RoleBinding\",\"factType\":\"StrandvoogdBeheertJutter\",\"role\":"
                + "\"beheerde jutter\"},{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"}],"
                + "\"type\":" + vouwType(kind, lid) + "}";
    }

    private static String vouwType(String kind, String lid) {
        return switch (lid) {
            case "vinddag", "bergdag" -> DATUM;
            case "vindtijdstip" -> TIJDSTIP;
            case "zoekduur" -> DAGEN;
            default -> GEHEEL;
        };
    }

    /** "het aantal vondsten van de Jutter". */
    private static String telling() {
        return "{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[" + eigen("vondsten", "Jutter") + "],\"elementType\":"
                + "\"Vondst\",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"JutterHeeftVondst\",\"role\":\"vondst\"},"
                + "\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}}";
    }

    /** A computing rule ("moet berekend worden als"). */
    private static String regel(String naam, String type, String doel, String waarde) {
        return versie(naam, type, doel, waarde, null, "calculation");
    }

    /** A computing rule written "moet gesteld worden op". */
    private static String gesteld(String naam, String type, String doel, String waarde) {
        return versie(naam, type, doel, waarde, null, "equation");
    }

    private static String initialisatie(String naam, String type, String doel, String waarde) {
        return versie(naam, type, doel, waarde, null, null);
    }

    private static String kenmerkRegel(String kenmerk, String type, String voorwaarde) {
        return versie(kenmerk, type, kenmerk, "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"}}",
                voorwaarde, "");
    }

    /** One rule with one version: {@code presentatie} a computing rule's tag, null for an initialisation, empty for a
     *  characteristic attribution. */
    private static String versie(String naam, String type, String doel, String waarde, String voorwaarde, String presentatie) {
        return "{\"node\":\"Rule\",\"name\":\"" + naam + "\",\"versions\":[{\"validity\":{\"node\":\"Validity\"},\"target\":"
                + eigen(doel, type) + ",\"value\":" + waarde + (voorwaarde == null ? "" : ",\"condition\":" + voorwaarde)
                + ",\"initialisation\":" + (presentatie == null)
                + (presentatie == null || presentatie.isEmpty() ? "" : ",\"presentation\":\"" + presentatie + "\"") + "}]}";
    }

    private static String document(String... regels) {
        return "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels) + "],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],"
                + "\"factCreations\":[],\"startPointDeterminations\":[],\"ruleGroups\":[]},\"diagnostics\":[]}";
    }

    // ---------------- cases ----------------

    /** The situation of every row: four Jutters with the four source shapes, and four Strandvoogden.
     *  J1: V1 12-03-2026 (gewicht 4, zoekduur 10), V2 20-03-2026 (7, 3), V3 without vinddag (6, 20), V4 05-03-2026 (2);
     *  J2 no vondsten; J3 V5 without vinddag and gewicht; J4 V6 31-12-2025 (9, 5). V2, V3 and V6 zwaar when
     *  {@code zwaar} is set; the vindtijdstip when {@code tijdstip} is set; J4's own eerste vinddag when given. */
    private static List<String> situatie(boolean zwaar, boolean tijdstip, String eigenEersteVinddagJ4) {
        List<String> uit = new ArrayList<>();
        uit.add(instantie("S1", "Strandvoogd", "\"bonus\":{\"number\":\"100\"}", "\"beheerde jutters\":[\"J1\",\"J3\"]", false));
        uit.add(instantie("S2", "Strandvoogd", "", "\"beheerde jutters\":[\"J2\"]", false));
        uit.add(instantie("S3", "Strandvoogd", "", "\"beheerde jutters\":[\"J4\"]", false));
        uit.add(instantie("S4", "Strandvoogd", "", "", false));
        uit.add(instantie("J1", "Jutter", "\"startdag\":" + datum("2026-03-01"), "", false));
        uit.add(instantie("J2", "Jutter", "\"startdag\":" + datum("2026-03-01"), "", false));
        uit.add(instantie("J3", "Jutter", "", "", false));
        uit.add(instantie("J4", "Jutter", (eigenEersteVinddagJ4 == null ? "" : "\"eerste vinddag\":" + datum(eigenEersteVinddagJ4) + ",")
                + "\"startdag\":" + datum("2026-01-01"), "", false));
        uit.add(vondst("V1", "J1", "2026-03-12", "4", "10", false, tijdstip ? "2026-03-12T10:00:00.000" : null));
        uit.add(vondst("V2", "J1", "2026-03-20", "7", "3", zwaar, tijdstip ? "2026-03-20T08:30:00.000" : null));
        uit.add(vondst("V3", "J1", null, "6", "20", zwaar, null));
        uit.add(vondst("V4", "J1", "2026-03-05", "2", null, false, tijdstip ? "2026-03-05T23:59:59.999" : null));
        uit.add(vondst("V5", "J3", null, null, null, false, null));
        uit.add(vondst("V6", "J4", "2025-12-31", "9", "5", zwaar, tijdstip ? "2025-12-31T00:00:00.001" : null));
        return uit;
    }

    private static String vondst(String label, String jutter, String vinddag, String gewicht, String zoekduur, boolean zwaar,
                                 String tijdstip) {
        List<String> a = new ArrayList<>();
        if (vinddag != null) a.add("\"vinddag\":" + datum(vinddag));
        if (gewicht != null) a.add("\"gewicht\":" + getal(gewicht));
        if (zoekduur != null) a.add("\"zoekduur\":" + getal(zoekduur));
        if (tijdstip != null) a.add("\"vindtijdstip\":" + tijdstip(tijdstip));
        return instantie(label, "Vondst", String.join(",", a), "\"jutter\":[\"" + jutter + "\"]", zwaar);
    }

    private static String instantie(String label, String type, String attributen, String rollen, boolean zwaar) {
        return "{\"label\":\"" + label + "\",\"objectType\":\"" + type + "\",\"attributes\":{" + attributen + "},\"roles\":{"
                + rollen + "},\"characteristics\":[" + (zwaar ? "\"zwaar\"" : "") + "]}";
    }

    private static String datum(String iso) { return "{\"date\":\"" + iso + "\"}"; }

    private static String tijdstip(String iso) { return "{\"dateTime\":\"" + iso + "\"}"; }

    private static String getal(String n) { return "{\"number\":\"" + n + "\"}"; }

    /** One expectation per Jutter J1–J4 on {@code attribuut}; a null value expects leeg. */
    private static List<String> perJutter(String attribuut, String... waarden) {
        return per(List.of("J1", "J2", "J3", "J4"), attribuut, waarden);
    }

    private static List<String> perVoogd(String attribuut, String... waarden) {
        return per(List.of("S1", "S2", "S3", "S4"), attribuut, waarden);
    }

    private static List<String> per(List<String> labels, String attribuut, String... waarden) {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++)
            uit.add(waarden[i] == null
                    ? "{\"kind\":\"attributeEmpty\",\"label\":\"" + labels.get(i) + "\",\"attribute\":\"" + attribuut + "\"}"
                    : "{\"kind\":\"attributeValue\",\"label\":\"" + labels.get(i) + "\",\"attribute\":\"" + attribuut
                      + "\",\"value\":" + waarden[i] + "}");
        return uit;
    }

    @SafeVarargs
    private static String zaak(List<String> instanties, List<String>... verwacht) {
        List<String> alle = new ArrayList<>();
        for (List<String> v : verwacht) alle.addAll(v);
        return "{\"format\":\"uchar-case-v0\",\"asOfDate\":null,\"input\":{\"instances\":[" + String.join(",", instanties)
                + "],\"parameters\":{}},\"expected\":[" + String.join(",", alle) + "]}";
    }

    private static Path tijdelijk(String ir) throws Exception {
        Path tmp = Files.createTempFile("datumvouw-over-rol", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir);
        return tmp;
    }

    /** The exec-model branch's verdict on one case: null when every expectation holds. */
    private static String speel(String ir, String zaak) throws Exception {
        return CaseRunner.speelCase(tijdelijk(ir), JSON.readTree(zaak), false);
    }
}
