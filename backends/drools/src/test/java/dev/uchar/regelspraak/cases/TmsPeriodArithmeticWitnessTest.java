package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.parameterRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.always;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.characteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.openFrom;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.openTo;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.ownTotal;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The period arithmetic of RUNTIME-SEMANTICS ch. 3.4 and 3.5 as the value of a rule on a temporal attribute, over all
 * time (ch. 3.1), on both branches of the lowering with truth maintenance ({@code PeriodArithmetic}), and the open
 * bounds it writes and the case reads, with the routes of the temporal total that meet one (ch. 3.6, EF-036).
 *
 * <p><b>A, plus and min, nested, with a scalar operand, and the total over the result.</b> "Het maandloon … als zijn
 * strandloon plus zijn stormtoeslag"; "het verschil … als zijn maandloon min (zijn stormtoeslag min zijn toeslag)";
 * "het opgehoogde … als zijn toeslag plus zijn verschil"; "het jaarloon … als het totaal van zijn maandloon". Each of
 * the last three reads what another rule derives. J1 (strandloon 100 on the first half of 2024, stormtoeslag 30 on
 * April–September, toeslag 5), by hand: maandloon 0 until 2024-01-01, 100, 130, 30, 0 from 2024-10-01 (ch. 3.4's
 * first example); stormtoeslag min toeslag −5, 25 on April–September, −5; verschil 5 until 2024-01-01, 105 on
 * January–March and 105 on April–June, one segment, 5 on July–September and 5 from October, one segment; opgehoogd 10,
 * 110, 10; jaarloon 3 × 100 + 3 × 130 + 3 × 30 = 780, the two open segments of maandloon adding 0. J2 (no timeline,
 * no toeslag): every timeline 0 on the one segment (−∞, +∞), the leeg scalar the empty timeline, jaarloon 0.
 *
 * <p><b>B, the sum per period, with and without the fallback, over members whose timelines a rule derives.</b> "De
 * netto van een Vondst … als zijn bruto min zijn korting"; "de nettosom van een Jutter … als de som van de netto van alle
 * vondsten van de Jutter"; "de brutosom … als de som van de bruto van alle vondsten"; "de gewaarborgde … als de som van
 * de netto van alle vondsten … of 0 als die er niet zijn". J1 with V1 (bruto 50 in January and 60 in February 2024,
 * korting 10 in February) and V2 (bruto 10 in February and in March): V1 netto 0, 50 on January–February, 0 from
 * March; V2 netto 0, 10 on February–March, 0 from April; nettosom and gewaarborgde 0, 50, 60, 10, 0 from April;
 * brutosom 50, 70, 10 and a gap outside (ch. 3.5's first example). J2 without vondsten: nettosom and brutosom are the
 * empty timeline, which their standing targets do not write (ch. 7.8), and gewaarborgde 0 on all time. J3 with V3
 * (bruto 40 in January) and V4 (bruto 20 in March): brutosom 40 and 20 with February a gap, gewaarborgde 0, 40, 0, 20,
 * 0 from April (ch. 3.5's second example).
 *
 * <p><b>C, the open bound in the routes of the total.</b> The total over "5 plus zijn strandloon" faults with EF-036
 * on its open segments of 5 (red case {@code jutterswet-totaal-open-segment}); the clip of a total by a negated
 * characteristic keeps the open bound of an input segment it cuts, so the total faults alike, and over open segments
 * of 0 it is 2 × 10 + 1 × 10 = 30; the per-element total faults naming the member whose timeline has an open segment
 * of 5, and adds an open segment of 0 as 0, 2 × 5 + 3 = 13; a period-arithmetic operand and a member of a sum per
 * period given as the asOfDate-bucket scalar fault in the reference interpreter's words; the whole-period qualifier
 * over an open segment has no end to walk to (ch. 3.1); a segment that does not fit the decimals of the target faults
 * at the write boundary (ch. 2.4, EF-006).
 *
 * <p><b>D, the case reader.</b> A segment without {@code from} after another is out of order; a mismatch names an open
 * bound as {@code -inf} or {@code +inf}.
 *
 * <p><b>E, what the branch refuses by name</b>, each in the same sentence on both branches.
 *
 * <p><b>Measured</b> 2026-11-03 on the documents of {@link #main}, A, B and the two plays of C: the verifier, the
 * reference interpreter's runner and the ASP runner pass all four. <b>Red</b> on 53ab929d5, before the period arithmetic was carried:
 * A, B, C and E on "EXCLUDE DROOLS-105: … die geen tijdsevenredig deel en geen dagentelling is …", D on the case
 * reader's "segment op 'strandloon' mist een 'from'-datum".
 */
class TmsPeriodArithmeticWitnessTest {

    // ---------------- A: plus and min ----------------

    static final String MODEL_A = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("strandloon", WHOLE, "month"), timeAttribute("stormtoeslag", WHOLE, "month"),
            TmsDocuments.attribute("toeslag", WHOLE), timeAttribute("maandloon", WHOLE, "month"),
            timeAttribute("verschil", WHOLE, "month"), timeAttribute("opgehoogd", WHOLE, "month"),
            TmsDocuments.attribute("jaarloon", WHOLE)), List.of(characteristic("actief")))), List.of());

    static final String MAANDLOON = computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
            binary("plus", ownRead("strandloon", "Jutter"), ownRead("stormtoeslag", "Jutter")), "null");
    static final String VERSCHIL = computingRule("verschil", "BEREKENING", "Jutter", "verschil",
            binary("minus", ownRead("maandloon", "Jutter"),
                    binary("minus", ownRead("stormtoeslag", "Jutter"), ownRead("toeslag", "Jutter"))), "null");
    static final String OPGEHOOGD = computingRule("opgehoogd", "BEREKENING", "Jutter", "opgehoogd",
            binary("plus", ownRead("toeslag", "Jutter"), ownRead("verschil", "Jutter")), "null");
    static final String JAARLOON = computingRule("jaarloon", "BEREKENING", "Jutter", "jaarloon",
            ownTotal("maandloon", "Jutter", "null"), "null");

    static final String STRANDLOON = timeline(List.of(segment("2024-01-01", "2024-07-01", "100")));
    static final String STORMTOESLAG = timeline(List.of(segment("2024-04-01", "2024-10-01", "30")));

    static JsonNode caseA() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "stormtoeslag", STORMTOESLAG,
                                "toeslag", numberValue("5")), List.of(), Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "maandloon", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-04-01", "100"), segment("2024-04-01", "2024-07-01", "130"),
                                segment("2024-07-01", "2024-10-01", "30"), openFrom("2024-10-01", "0"))),
                        expectedTimeline("J1", "verschil", List.of(openTo("2024-01-01", "5"),
                                segment("2024-01-01", "2024-07-01", "105"), openFrom("2024-07-01", "5"))),
                        expectedTimeline("J1", "opgehoogd", List.of(openTo("2024-01-01", "10"),
                                segment("2024-01-01", "2024-07-01", "110"), openFrom("2024-07-01", "10"))),
                        value("J1", "jaarloon", "780"),
                        expectedTimeline("J2", "maandloon", List.of(always("0"))),
                        expectedTimeline("J2", "verschil", List.of(always("0"))),
                        expectedTimeline("J2", "opgehoogd", List.of(always("0"))),
                        value("J2", "jaarloon", "0"), consistent()));
    }

    @Test void plus_and_min_nested_with_a_scalar_and_the_total_over_them_in_every_order_on_both_branches() throws Exception {
        assertEquals(gespeeld(4 * 3 * 2), inEveryOrder(MODEL_A, List.of(MAANDLOON, VERSCHIL, OPGEHOOGD, JAARLOON), caseA()));
    }

    // ---------------- B: the sum per period ----------------

    static final String MODEL_B = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(timeAttribute("nettosom", WHOLE, "month"),
                    timeAttribute("brutosom", WHOLE, "month"), timeAttribute("gewaarborgde", WHOLE, "month")), List.of()),
            objectType("Vondst", List.of(timeAttribute("bruto", WHOLE, "month"), timeAttribute("korting", WHOLE, "month"),
                    timeAttribute("netto", WHOLE, "month")), List.of(characteristic("gemeld")))),
            List.of(factTypeOneToMany("aanmelding", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));

    static String sum(String attribute, String fallback, String filter) {
        return aggregation("sum", ownRead(attribute, "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"), filter,
                fallback, WHOLE, "de som van de " + attribute + " van alle vondsten van de Jutter");
    }

    static final String NETTO = computingRule("netto", "BEREKENING", "Vondst", "netto",
            binary("minus", ownRead("bruto", "Vondst"), ownRead("korting", "Vondst")), "null");
    static final String NETTOSOM = computingRule("nettosom", "BEREKENING", "Jutter", "nettosom",
            sum("netto", "null", "null"), "null");
    static final String BRUTOSOM = computingRule("brutosom", "BEREKENING", "Jutter", "brutosom",
            sum("bruto", "null", "null"), "null");
    static final String GEWAARBORGDE = computingRule("gewaarborgde", "BEREKENING", "Jutter", "gewaarborgde",
            sum("netto", number("0"), "null"), "null");

    static String vondst(String label, Map<String, String> values) {
        return instance(label, "Vondst", values, List.of(), Map.of(), Map.of());
    }

    static JsonNode caseB() throws Exception {
        List<String> j1 = List.of(openTo("2024-01-01", "0"), segment("2024-01-01", "2024-02-01", "50"),
                segment("2024-02-01", "2024-03-01", "60"), segment("2024-03-01", "2024-04-01", "10"),
                openFrom("2024-04-01", "0"));
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V1", "V2"))),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of()),
                        instance("J3", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V3", "V4"))),
                        vondst("V1", Map.of("bruto", timeline(List.of(segment("2024-01-01", "2024-02-01", "50"),
                                        segment("2024-02-01", "2024-03-01", "60"))),
                                "korting", timeline(List.of(segment("2024-02-01", "2024-03-01", "10"))))),
                        vondst("V2", Map.of("bruto", timeline(List.of(segment("2024-02-01", "2024-03-01", "10"),
                                segment("2024-03-01", "2024-04-01", "10"))))),
                        vondst("V3", Map.of("bruto", timeline(List.of(segment("2024-01-01", "2024-02-01", "40"))))),
                        vondst("V4", Map.of("bruto", timeline(List.of(segment("2024-03-01", "2024-04-01", "20")))))),
                List.of(expectedTimeline("V1", "netto", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-03-01", "50"), openFrom("2024-03-01", "0"))),
                        expectedTimeline("V2", "netto", List.of(openTo("2024-02-01", "0"),
                                segment("2024-02-01", "2024-04-01", "10"), openFrom("2024-04-01", "0"))),
                        expectedTimeline("J1", "nettosom", j1), expectedTimeline("J1", "gewaarborgde", j1),
                        expectedTimeline("J1", "brutosom", List.of(segment("2024-01-01", "2024-02-01", "50"),
                                segment("2024-02-01", "2024-03-01", "70"), segment("2024-03-01", "2024-04-01", "10"))),
                        expectedTimeline("J2", "nettosom", List.of()), expectedTimeline("J2", "brutosom", List.of()),
                        expectedTimeline("J2", "gewaarborgde", List.of(always("0"))),
                        expectedTimeline("J3", "brutosom", List.of(segment("2024-01-01", "2024-02-01", "40"),
                                segment("2024-03-01", "2024-04-01", "20"))),
                        expectedTimeline("J3", "gewaarborgde", List.of(openTo("2024-01-01", "0"),
                                segment("2024-01-01", "2024-02-01", "40"), segment("2024-02-01", "2024-03-01", "0"),
                                segment("2024-03-01", "2024-04-01", "20"), openFrom("2024-04-01", "0"))),
                        consistent()));
    }

    @Test void the_sum_per_period_with_and_without_the_fallback_in_every_order_on_both_branches() throws Exception {
        assertEquals(gespeeld(4 * 3 * 2), inEveryOrder(MODEL_B, List.of(NETTO, NETTOSOM, BRUTOSOM, GEWAARBORGDE), caseB()));
    }

    /** The staggered contributors of ch. 3.5 as the value of a timeline target, without a fallback: until this branch
     *  carried the sum per period it refused this document by name ({@code TmsTimelineRefusalWitnessTest}, the row
     *  {@code som-over-tijdlijnen}, DROOLS-105). */
    @Test void the_sum_per_period_that_the_branch_refused_plays() throws Exception {
        Path ir = document(TmsDocuments.model(List.of(
                        objectType("Jutter", List.of(timeAttribute("tariefsom", WHOLE, "month")), List.of()),
                        objectType("Vondst", List.of(timeAttribute("tarief", WHOLE, "month")), List.of())),
                List.of(factTypeOneToMany("bewaring", "jutter", "Jutter", "vondst", "Vondst", "vondsten"))),
                List.of(computingRule("tariefsom", "BEREKENING", "Jutter", "tariefsom", aggregation("sum",
                        ownRead("tarief", "Vondst"), "Vondst", sourceRole("bewaring", "vondst"), "null", "null",
                        WHOLE, "de som van de tarieven van zijn vondsten"), "null")));
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V1", "V2"))),
                        instance("V1", "Vondst", Map.of("tarief", timeline(List.of(segment("2024-01-01", "2024-02-01", "50"),
                                segment("2024-02-01", "2024-03-01", "60")))), List.of(), Map.of(), Map.of()),
                        instance("V2", "Vondst", Map.of("tarief", timeline(List.of(segment("2024-02-01", "2024-03-01", "10"),
                                segment("2024-03-01", "2024-04-01", "10")))), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "tariefsom", List.of(segment("2024-01-01", "2024-02-01", "50"),
                        segment("2024-02-01", "2024-03-01", "70"), segment("2024-03-01", "2024-04-01", "10")))));
        BothBranches.green(ir, c, "the staggered contributors of ch. 3.5, 50, 70, 10");
    }

    // ---------------- C: the open bound in the routes of the total ----------------

    @Test void the_own_total_over_an_open_segment_of_another_value_than_0_faults_in_every_order() throws Exception {
        String model = TmsDocuments.model(List.of(objectType("Jutter", List.of(timeAttribute("strandloon", WHOLE, "month"),
                timeAttribute("opgehoogd", WHOLE, "month"), TmsDocuments.attribute("jaarloon", WHOLE)), List.of())), List.of());
        List<String> rules = List.of(computingRule("opgehoogd", "BEREKENING", "Jutter", "opgehoogd",
                        binary("plus", number("5"), ownRead("strandloon", "Jutter")), "null"),
                computingRule("jaarloon", "BEREKENING", "Jutter", "jaarloon", ownTotal("opgehoogd", "Jutter", "null"),
                        "null"));
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())), List.of(consistent()));
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 2 * 2; i++) expected.add(EF_036_JAARLOON);
        assertEquals(expected, outcomesInEveryOrder(model, rules, c));
    }

    static final String EF_036_JAARLOON = "OpenSegmentFault: fout in de regel \"jaarloon\": het temporele totaal van"
            + " \"opgehoogd\" op \"J1\" raakt een segment met een open grens dat 5 draagt: een andere waarde dan 0 over"
            + " onbegrensd veel eenheden heeft geen totaal (ch. 3.6)";

    static final String MODEL_CLIP = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("bedrag", WHOLE, "month"), TmsDocuments.attribute("wachtsom", WHOLE)),
            List.of(timeCharacteristic("borgplichtig", "month")))), List.of());
    static final String WACHTSOM_NIET = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            ownTotal("bedrag", "Jutter", check("borgplichtig", true)), "null");

    static JsonNode clipCase(String before, String expected) throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("bedrag", timeline(List.of(
                        openTo("2024-01-01", before), segment("2024-01-01", "2024-07-01", "10"), openFrom("2024-07-01", "0")))),
                List.of(), Map.of("borgplichtig", List.of(period("2024-03-01", "2024-06-01"))), Map.of())),
                expected == null ? List.of(consistent()) : List.of(value("J1", "wachtsom", expected), consistent()));
    }

    @Test void the_clip_keeps_the_open_bound_it_cuts_and_its_total_faults_on_another_value_than_0() throws Exception {
        Path ir = document(MODEL_CLIP, List.of(WACHTSOM_NIET));
        assertEquals("OpenSegmentFault: fout in de regel \"wachtsom\": het temporele totaal van \"bedrag\" op \"J1\" raakt"
                + " een segment met een open grens dat 7 draagt: een andere waarde dan 0 over onbegrensd veel eenheden heeft"
                + " geen totaal (ch. 3.6)", TmsFaultWitnessTest.playBoth(ir, clipCase("7", null)));
        BothBranches.green(ir, clipCase("0", "30"), "January, February and June outside borgplichtig, 30");
    }

    static final String MODEL_SET = TmsDocuments.model(List.of(
            objectType("Jutter", List.of(TmsDocuments.attribute("vondsttotaal", WHOLE),
                    timeAttribute("vondstsom", WHOLE, "month")), List.of()),
            objectType("Vondst", List.of(timeAttribute("opbrengst", WHOLE, "month")), List.of())),
            List.of(factTypeOneToMany("aanmelding", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));
    static final String VONDSTTOTAAL = computingRule("vondsttotaal", "BEREKENING", "Jutter", "vondsttotaal",
            aggregation("total", ownRead("opbrengst", "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"), "null",
                    "null", WHOLE, "het totaal van de opbrengst van alle vondsten van de Jutter"), "null");

    static JsonNode setCase(String v1, String v2, List<String> expectations) throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                instance("J1", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V1", "V2"))),
                instance("V1", "Vondst", Map.of("opbrengst", v1), List.of(), Map.of(), Map.of()),
                instance("V2", "Vondst", Map.of("opbrengst", v2), List.of(), Map.of(), Map.of())), expectations);
    }

    @Test void the_per_element_total_faults_naming_the_member_with_an_open_segment_and_adds_one_of_0_as_0() throws Exception {
        Path ir = document(MODEL_SET, List.of(VONDSTTOTAAL));
        String v2 = timeline(List.of(segment("2024-01-01", "2024-02-01", "3")));
        assertEquals("OpenSegmentFault: fout in de regel \"vondsttotaal\": het temporele totaal van \"opbrengst\" op \"V1\""
                + " raakt een segment met een open grens dat 5 draagt: een andere waarde dan 0 over onbegrensd veel"
                + " eenheden heeft geen totaal (ch. 3.6)", TmsFaultWitnessTest.playBoth(ir,
                setCase(timeline(List.of(openTo("2024-02-01", "5"))), v2, List.of(consistent()))));
        BothBranches.green(ir, setCase(timeline(List.of(openTo("2024-02-01", "0"), segment("2024-02-01", "2024-04-01", "5"))),
                v2, List.of(value("J1", "vondsttotaal", "13"), consistent())), "2 × 5 + 3, the open 0 adds 0");
    }

    @Test void an_operand_and_a_member_given_as_the_bucket_scalar_fault_in_the_words_of_the_reference_interpreter() throws Exception {
        JsonNode bucket = TmsTimelineDocuments.caseDocument("2024-06-01", List.of(instance("J1", "Jutter",
                Map.of("strandloon", numberValue("100")), List.of(), Map.of(), Map.of())), List.of(consistent()));
        assertEquals("InputFormFault: fout in de regel \"maandloon\": the period arithmetic of ch. 3.4/3.5 reads the"
                + " temporal attribute \"strandloon\" of \"J1\"; the case supplies only the asOfDate-bucket scalar, which"
                + " carries no period", TmsFaultWitnessTest.playBoth(document(MODEL_A, List.of(MAANDLOON)), bucket));
        Path ir = document(MODEL_SET, List.of(computingRule("vondstsom", "BEREKENING", "Jutter", "vondstsom",
                aggregation("sum", ownRead("opbrengst", "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"), "null",
                        "null", WHOLE, "de som van de opbrengst van alle vondsten van de Jutter"), "null")));
        JsonNode member = TmsTimelineDocuments.caseDocument("2024-06-01", List.of(
                instance("J1", "Jutter", Map.of(), List.of(), Map.of(), Map.of("vondsten", List.of("V1", "V2"))),
                instance("V1", "Vondst", Map.of("opbrengst", timeline(List.of(segment("2024-01-01", "2024-02-01", "3")))),
                        List.of(), Map.of(), Map.of()),
                instance("V2", "Vondst", Map.of("opbrengst", numberValue("4")), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
        assertEquals("InputFormFault: fout in de regel \"vondstsom\": the period arithmetic of ch. 3.4/3.5 reads the"
                + " temporal attribute \"opbrengst\" of \"V2\"; the case supplies only the asOfDate-bucket scalar, which"
                + " carries no period", TmsFaultWitnessTest.playBoth(ir, member));
    }

    @Test void the_whole_period_qualifier_over_an_open_segment_has_no_end_to_walk_to() throws Exception {
        String model = TmsDocuments.model(List.of(objectType("Jutter", List.of(timeAttribute("bedrag", WHOLE, "month")),
                List.of(characteristic("vast")))), List.of());
        Path ir = document(model, List.of(TmsDocuments.characteristicRule("vast", "Jutter", "vast",
                TmsWholePeriodWitnessTest.wholeComparison("greater", ownRead("bedrag", "Jutter"), number("5"),
                        TmsWholePeriodWitnessTest.whole(true, false), "zijn bedrag gedurende het gehele jaar groter dan 5"))));
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("bedrag",
                timeline(List.of(openTo("2024-01-01", "10"), segment("2024-01-01", "2025-01-01", "10")))), List.of(),
                Map.of(), Map.of())), List.of(consistent()));
        assertEquals("ComputeFault: fout in de regel \"vast\": a fold over the buckets of a grid walks the buckets of a"
                + " period with an open bound, which has no end to reach (ch. 3.1)", TmsFaultWitnessTest.playBoth(ir, c));
    }

    @Test void a_segment_that_does_not_fit_the_decimals_of_the_target_faults_at_the_write_boundary() throws Exception {
        String model = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                timeAttribute("strandloon", TmsDocuments.numberType(false, 2, null), "month"),
                timeAttribute("maandloon", WHOLE, "month")), List.of())), List.of());
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("strandloon",
                timeline(List.of(segment("2024-01-01", "2024-07-01", "100.50")))), List.of(), Map.of(), Map.of())),
                List.of(consistent()));
        // 5 on the open segments fits; 100,50 plus 5 on the first half of 2024 does not
        assertEquals("WriteBoundaryFault: fout in de regel \"maandloon\": J1.maandloon: de waarde 211/2 past niet in de"
                + " gedeclareerde 0 decimalen — een expliciete afronding is verplicht (RUNTIME-SEMANTICS ch. 2.4)",
                TmsFaultWitnessTest.playBoth(document(model, List.of(MAANDLOON_OF_STRANDLOON)), c));
    }

    // ---------------- D: the case reader ----------------

    @Test void the_case_reader_holds_the_order_of_open_bounds_and_names_them_in_a_mismatch() throws Exception {
        Path ir = document(MODEL_A, List.of(MAANDLOON));
        JsonNode misordered = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("strandloon",
                timeline(List.of(segment("2024-01-01", "2024-07-01", "100"), openTo("2024-09-01", "5")))), List.of(),
                Map.of(), Map.of())), List.of(consistent()));
        assertEquals("IllegalArgumentException: tijdlijnsegmenten op 'strandloon' zijn ongeordend of overlappen",
                TmsFaultWitnessTest.playBoth(ir, misordered));
        JsonNode mismatch = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())), List.of(expectedTimeline("J1",
                "maandloon", List.of(always("0")))));
        assertEquals("case: attribuut maandloon: verwacht [-inf,+inf)=0, werkelijk [-inf,2024-01-01)=0"
                + " [2024-01-01,2024-07-01)=100 [2024-07-01,+inf)=0", TmsFaultWitnessTest.playBoth(ir, mismatch));
    }

    // ---------------- E: what the branch refuses by name ----------------

    @Test void every_form_outside_the_period_arithmetic_it_carries_is_refused_by_name_on_both_branches() throws Exception {
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())), List.of(consistent()));
        // a condition on the rule, which would restrict the timeline per period
        BothBranches.refused(document(MODEL_A, List.of(computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
                        binary("plus", ownRead("strandloon", "Jutter"), ownRead("stormtoeslag", "Jutter")),
                        check("actief", false)))), c,
                "regel: maandloon — de regel \"maandloon\" schrijft het tijdsafhankelijke attribuut 'maandloon' van Jutter"
                        + " met het rekenen per periode van ch. 3.4/3.5 onder een voorwaarde", "DROOLS-302");
        // another operator below plus
        BothBranches.refused(document(MODEL_A, List.of(computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
                        binary("plus", ownRead("strandloon", "Jutter"), binary("times", ownRead("stormtoeslag", "Jutter"),
                                number("2"))), "null"))), c,
                "regel: maandloon — deze operand van het rekenen per periode van ch. 3.4/3.5 wordt niet uitgevoerd ('times')",
                "DROOLS-303");
        // a time-dependent parameter as an operand
        String withParameter = TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                        timeAttribute("strandloon", WHOLE, "month"), timeAttribute("maandloon", WHOLE, "month")), List.of())),
                List.of(), List.of("{\"node\":\"Parameter\",\"name\":\"tarief\",\"datatype\":" + WHOLE + ",\"timeline\":"
                        + "{\"node\":\"TimelineFacet\",\"granularity\":\"month\",\"count\":1}}"), List.of());
        BothBranches.refused(document(withParameter, List.of(computingRule("maandloon", "BEREKENING", "Jutter",
                        "maandloon", binary("plus", ownRead("strandloon", "Jutter"), parameterRead("tarief")), "null"))), c,
                "regel: maandloon — de tijdsafhankelijke parameter 'tarief' als operand van het rekenen per periode",
                "DROOLS-304");
        // a die/dat subselection on the sum
        JsonNode set = setCase(timeline(List.of(segment("2024-01-01", "2024-02-01", "3"))),
                timeline(List.of(segment("2024-01-01", "2024-02-01", "3"))), List.of(consistent()));
        String gemeld = TmsDocuments.model(List.of(
                objectType("Jutter", List.of(timeAttribute("vondstsom", WHOLE, "month")), List.of()),
                objectType("Vondst", List.of(timeAttribute("opbrengst", WHOLE, "month")), List.of(characteristic("gemeld")))),
                List.of(factTypeOneToMany("aanmelding", "jutter", "Jutter", "vondst", "Vondst", "vondsten")));
        BothBranches.refused(document(gemeld, List.of(computingRule("vondstsom", "BEREKENING", "Jutter", "vondstsom",
                        aggregation("sum", ownRead("opbrengst", "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"),
                                check("gemeld", false), "null", WHOLE, "de som van de opbrengst van alle gemelde vondsten"),
                        "null"))), set,
                "regel: vondstsom — een die/dat-subselectie op de som per periode ('de som van de opbrengst van alle"
                        + " gemelde vondsten') wordt niet uitgevoerd", "DROOLS-305");
        // a restatement between two units
        String eur = TmsDocuments.unit("EUR", "Munt", "EUR", 1, 1), ct = TmsDocuments.unit("ct", "Munt", "EUR", 1, 100);
        String units = TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                        timeAttribute("strandloon", TmsDocuments.numberType(true, 0, ct), "month"),
                        timeAttribute("maandloon", TmsDocuments.numberType(true, 0, eur), "month")), List.of())),
                List.of(), List.of(), List.of(TmsDocuments.unitSystem("Munt", List.of(eur, ct))));
        BothBranches.refused(document(units, List.of(computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
                        binary("plus", ownRead("strandloon", "Jutter"), number("5")), "null"))), c,
                "regel: maandloon — het rekenen per periode van ch. 3.4/3.5 herrekent de operand 'zijn strandloon'",
                "DROOLS-306");
        // a target of the unbounded form
        String unbounded = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                timeAttribute("strandloon", WHOLE, "month"),
                timeAttribute("maandloon", TmsDocuments.numberType(false, 0, null), "month")), List.of())), List.of());
        BothBranches.refused(document(unbounded, List.of(MAANDLOON_OF_STRANDLOON)), c,
                "regel: maandloon — het rekenen per periode van ch. 3.4/3.5 over 'plus', een waarde van de onbegrensde vorm",
                "DROOLS-307");
        // an operand on a finer grid than the target's
        String year = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                timeAttribute("strandloon", WHOLE, "month"), timeAttribute("maandloon", WHOLE, "year")), List.of())), List.of());
        BothBranches.refused(document(year, List.of(MAANDLOON_OF_STRANDLOON)), c,
                "regel: maandloon — de operand 'strandloon' van het rekenen per periode van ch. 3.4/3.5 ligt op een raster"
                        + " van MAAND, fijner dan het JAAR-raster van het doel", "DROOLS-308");
        // another operator as the whole value
        BothBranches.refused(document(MODEL_A, List.of(computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
                        binary("times", ownRead("strandloon", "Jutter"), ownRead("stormtoeslag", "Jutter")), "null"))), c,
                "regel: maandloon — een waarde van het tijdsafhankelijke doel 'maandloon' van Jutter die geen tijdsevenredig"
                        + " deel, geen dagentelling en geen rekenen per periode met plus, min of een som is ('times')",
                "DROOLS-105");
    }

    static final String MAANDLOON_OF_STRANDLOON = computingRule("maandloon", "BEREKENING", "Jutter", "maandloon",
            binary("plus", ownRead("strandloon", "Jutter"), number("5")), "null");

    // ---------------- helpers ----------------

    static Path document(String model, List<String> rules) throws Exception {
        return TmsTimelineDocuments.document(model, rules, List.of());
    }

    /** {@code count} times "gespeeld", the outcome of a play that matches. */
    static List<String> gespeeld(int count) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add("exec-model gespeeld");
            out.add("drl-tekst gespeeld");
        }
        return out;
    }

    /** The outcome of {@code caseDocument} on the document of {@code model} with {@code rules} in every order of the
     *  rules, on the exec-model branch and on the DRL-text branch, each as "branch outcome"
     *  ({@link TmsFaultWitnessTest#play}). */
    static List<String> inEveryOrder(String model, List<String> rules, JsonNode caseDocument) throws Exception {
        List<String> out = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(rules)) {
            Path ir = document(model, order);
            out.add("exec-model " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.EXEC_MODEL));
            out.add("drl-tekst " + TmsFaultWitnessTest.play(ir, caseDocument, DroolsBackend.Branch.DRL_TEXT));
        }
        return out;
    }

    /** As {@link #inEveryOrder}, the outcomes without the branch. */
    static List<String> outcomesInEveryOrder(String model, List<String> rules, JsonNode caseDocument) throws Exception {
        List<String> out = new ArrayList<>();
        for (String o : inEveryOrder(model, rules, caseDocument)) out.add(o.substring(o.indexOf(' ') + 1));
        return out;
    }

    /** Writes the documents of A, B and C with their cases under {@code args[0]}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        TmsTimelineWitnessTest.write(out.resolve("a-plus-min"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(MAANDLOON, VERSCHIL, OPGEHOOGD, JAARLOON), List.of()), caseA());
        TmsTimelineWitnessTest.write(out.resolve("b-som-per-periode"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(NETTO, NETTOSOM, BRUTOSOM, GEWAARBORGDE), List.of()), caseB());
        TmsTimelineWitnessTest.write(out.resolve("c-klip-nul"),
                TmsTimelineDocuments.ir(MODEL_CLIP, List.of(WACHTSOM_NIET), List.of()), clipCase("0", "30"));
        TmsTimelineWitnessTest.write(out.resolve("c-vouw-nul"),
                TmsTimelineDocuments.ir(MODEL_SET, List.of(VONDSTTOTAAL), List.of()),
                setCase(timeline(List.of(openTo("2024-02-01", "0"), segment("2024-02-01", "2024-04-01", "5"))),
                        timeline(List.of(segment("2024-01-01", "2024-02-01", "3"))),
                        List.of(value("J1", "vondsttotaal", "13"), consistent())));
    }
}
