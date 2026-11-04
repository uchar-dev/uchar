package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import static dev.uchar.regelspraak.cases.TmsDocuments.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberType;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.parameterRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.rounding;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.unit;
import static dev.uchar.regelspraak.cases.TmsDocuments.unitSystem;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.always;
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
 * Every operator of the arithmetic table of RUNTIME-SEMANTICS ch. 1.2 but the power read per day over timelines, as the
 * value of a rule on a temporal attribute on a day grid (ch. 3.4), a time-dependent parameter as an operand (ch. 3.4,
 * 8.3), and the temporal total by the calendar between the day family and the month family of time units (ch. 3.6),
 * on both branches of the lowering with truth maintenance ({@code PeriodArithmetic}).
 *
 * <p><b>A, maal, verminderd met and percentage van, and the total over a day grid.</b> "Het loonproduct … als zijn
 * strandloon maal zijn stormtoeslag"; "het nettoloon … als zijn strandloon verminderd met zijn stormtoeslag"; "de heffing
 * (1 decimaal) … als 10% van zijn nettoloon"; "het jaarproduct … als het totaal van zijn loonproduct". J1 (strandloon
 * 100 on [2024-01-10, 2024-07-05), stormtoeslag 25 on [2024-04-03, 2024-10-01)), by hand: loonproduct 0 until
 * 2024-04-03, 2500, 0 from 2024-07-05; nettoloon 100 on [2024-01-10, 2024-04-03) and 75 up to 2024-07-05, a gap
 * outside, where the strandloon is leeg; heffing 0, 10, 7,5, 0 on all time, 10% of a leeg nettoloon being 0;
 * jaarproduct 2500 × 93 days = 232500. J2 (no timeline): loonproduct and heffing 0 on (−∞, +∞), nettoloon the empty
 * timeline, jaarproduct 0.
 *
 * <p><b>B, both divisions, their deposits and a target of the unbounded form.</b> "De deler … als zijn stormtoeslag
 * verminderd met zijn korting"; "de verhouding (getal) … als zijn strandloon gedeeld door zijn deler"; "de noemer … als
 * zijn stormtoeslag min 28"; "de afgekapte verhouding (5 decimalen) … als zijn strandloon gedeeld door (ABS) zijn
 * noemer". J1 adds korting 17 on [2024-04-03, 2024-06-01) and 25 up to 2024-10-01: deler 8 and 0 on those periods, a
 * gap outside; verhouding 0 until 2024-01-10 (a leeg dividend), leeg on [2024-01-10, 2024-04-03) (a leeg divisor,
 * EF-031), 12,5, leeg on [2024-06-01, 2024-07-05) (a zero divisor, EF-005), 0 from 2024-07-05, so one §9.5 fact of the
 * rule on J1 with both codes; noemer −28, −3 on [2024-04-03, 2024-10-01), −28; afgekapte verhouding 0, −3,57142
 * (100/−28 truncated), −33,33333, 0 from 2024-07-05. J2: deler empty, verhouding 0, noemer −28, afgekapt 0.
 *
 * <p><b>C, a time-dependent parameter and the calendar, clipped and not.</b> "De dagrente (€/jr, voor elke dag) … als de
 * rentevoet (1/jr, voor elke maand: 0,04 until 2026-02-01, 0,05 from then, both bounds open) maal zijn afdrachtbedrag";
 * "de verzuimrente … als (het totaal van zijn dagrente gedurende de tijd dat hij rentedragend is) rekenkundig afgerond
 * op 2 decimalen"; "het nettotarief (€/jr, voor elke dag) … als zijn tarief verminderd met zijn korting"; "de rente …
 * als (het totaal van zijn nettotarief) rekenkundig afgerond op 2 decimalen", the total without a clip over a timeline
 * the period arithmetic writes. Both Afdrachten have an afdrachtbedrag of 10 000 €, so the dagrente is 400 €/jr until
 * 2026-02-01 and 500 €/jr from then. A1 (rentedragend on [2028-03-01, 2028-03-11), tarief 500 €/jr on
 * [2028-02-11, 2028-04-05)): verzuimrente 500 × 10/366, 13,66; nettotarief 500 there, a gap outside; rente
 * 500 × 54/366 = 73 47/61, 73,77. A2 (rentedragend on [2025-11-16, 2026-03-13), the anchor's first case, no tarief):
 * verzuimrente 400 × 77/365 + 500 × 40/365 = 139 13/73, 139,18; nettotarief the empty timeline, rente 0. The calendar
 * on the route of the asOfDate bucket: 10 €/dg and 7 €/wk over the bucket of February 2024 on a month grid total 290 €
 * and 29 €.
 *
 * <p><b>D, what the branch refuses by name</b> and <b>E, the faults</b> of these forms and the case reader's hold on a
 * time-dependent parameter, each in the same sentence on both branches.
 *
 * <p><b>Measured</b> 2026-11-03 on the five documents of {@link #main}: the verifier passes all five and the reference
 * interpreter's runner gives the expectations here on all five; the ASP runner gives them on A and refuses the other
 * four by name, as limits of its encoding (a unit-carrying multiplicative operand; a quotient that is not whole in the
 * unbounded form; the calendar on the route of the bucket scalar; a calendar weight past its guarded range).
 */
class TmsPerDayArithmeticWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- A: maal, verminderd met, percentage van, the total over a day grid ----------------

    static final String MODEL_A = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("strandloon", WHOLE, "day"), timeAttribute("stormtoeslag", WHOLE, "day"),
            timeAttribute("loonproduct", WHOLE, "day"), timeAttribute("nettoloon", WHOLE, "day"),
            timeAttribute("heffing", numberType(false, 1, null), "day"), TmsDocuments.attribute("jaarproduct", WHOLE)),
            List.of())), List.of());

    static final String PRODUCT = computingRule("loonproduct", "BEREKENING", "Jutter", "loonproduct",
            binary("times", ownRead("strandloon", "Jutter"), ownRead("stormtoeslag", "Jutter"), numberType(false, 0, null)),
            "null");
    static final String NETTO = computingRule("nettoloon", "BEREKENING", "Jutter", "nettoloon",
            binary("reducedBy", ownRead("strandloon", "Jutter"), ownRead("stormtoeslag", "Jutter")), "null");
    static final String HEFFING = computingRule("heffing", "BEREKENING", "Jutter", "heffing",
            binary("percentageOf", number("10"), ownRead("nettoloon", "Jutter"), numberType(false, 0, null)), "null");
    static final String JAARPRODUCT = computingRule("jaarproduct", "BEREKENING", "Jutter", "jaarproduct",
            ownTotal("loonproduct", "Jutter", "null"), "null");

    static final String STRANDLOON = timeline(List.of(segment("2024-01-10", "2024-07-05", "100")));
    static final String STORMTOESLAG = timeline(List.of(segment("2024-04-03", "2024-10-01", "25")));

    static JsonNode caseA() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "stormtoeslag", STORMTOESLAG), List.of(),
                                Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "loonproduct", List.of(openTo("2024-04-03", "0"),
                                segment("2024-04-03", "2024-07-05", "2500"), openFrom("2024-07-05", "0"))),
                        expectedTimeline("J1", "nettoloon", List.of(segment("2024-01-10", "2024-04-03", "100"),
                                segment("2024-04-03", "2024-07-05", "75"))),
                        expectedTimeline("J1", "heffing", List.of(openTo("2024-01-10", "0"),
                                segment("2024-01-10", "2024-04-03", "10"), segment("2024-04-03", "2024-07-05", "7.5"),
                                openFrom("2024-07-05", "0"))),
                        value("J1", "jaarproduct", "232500"),
                        expectedTimeline("J2", "loonproduct", List.of(always("0"))),
                        expectedTimeline("J2", "nettoloon", List.of()),
                        expectedTimeline("J2", "heffing", List.of(always("0"))),
                        value("J2", "jaarproduct", "0"), consistent()));
    }

    @Test void maal_verminderd_met_percentage_van_and_the_total_over_a_day_grid_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(4 * 3 * 2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_A, List.of(PRODUCT, NETTO, HEFFING, JAARPRODUCT), caseA()));
    }

    // ---------------- B: both divisions, their deposits, a target of the unbounded form ----------------

    static final String MODEL_B = TmsDocuments.model(List.of(objectType("Jutter", List.of(
            timeAttribute("strandloon", WHOLE, "day"), timeAttribute("stormtoeslag", WHOLE, "day"),
            timeAttribute("korting", WHOLE, "day"), timeAttribute("deler", WHOLE, "day"),
            timeAttribute("verhouding", numberType(false, 0, null), "day"), timeAttribute("noemer", WHOLE, "day"),
            timeAttribute("afgekapt", numberType(false, 5, null), "day")), List.of())), List.of());

    static final String DELER = computingRule("deler", "BEREKENING", "Jutter", "deler",
            binary("reducedBy", ownRead("stormtoeslag", "Jutter"), ownRead("korting", "Jutter")), "null");
    static final String VERHOUDING = computingRule("verhouding", "BEREKENING", "Jutter", "verhouding",
            binary("dividedBy", ownRead("strandloon", "Jutter"), ownRead("deler", "Jutter"), numberType(false, 0, null)),
            "null");
    static final String NOEMER = computingRule("noemer", "BEREKENING", "Jutter", "noemer",
            binary("minus", ownRead("stormtoeslag", "Jutter"), number("28")), "null");
    static final String AFGEKAPT = computingRule("afgekapt", "BEREKENING", "Jutter", "afgekapt",
            binary("dividedByAbs", ownRead("strandloon", "Jutter"), ownRead("noemer", "Jutter"),
                    numberType(false, 0, null)), "null");

    static JsonNode caseB() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(
                        instance("J1", "Jutter", Map.of("strandloon", STRANDLOON, "stormtoeslag", STORMTOESLAG,
                                "korting", timeline(List.of(segment("2024-04-03", "2024-06-01", "17"),
                                        segment("2024-06-01", "2024-10-01", "25")))), List.of(), Map.of(), Map.of()),
                        instance("J2", "Jutter", Map.of(), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "deler", List.of(segment("2024-04-03", "2024-06-01", "8"),
                                segment("2024-06-01", "2024-10-01", "0"))),
                        expectedTimeline("J1", "verhouding", List.of(openTo("2024-01-10", "0"),
                                segment("2024-04-03", "2024-06-01", "12.5"), openFrom("2024-07-05", "0"))),
                        expectedTimeline("J1", "noemer", List.of(openTo("2024-04-03", "-28"),
                                segment("2024-04-03", "2024-10-01", "-3"), openFrom("2024-10-01", "-28"))),
                        expectedTimeline("J1", "afgekapt", List.of(openTo("2024-01-10", "0"),
                                segment("2024-01-10", "2024-04-03", "-3.57142"),
                                segment("2024-04-03", "2024-07-05", "-33.33333"), openFrom("2024-07-05", "0"))),
                        inconsistent("verhouding", "J1", "EF-031"), inconsistent("verhouding", "J1", "EF-005"),
                        expectedTimeline("J2", "deler", List.of()),
                        expectedTimeline("J2", "verhouding", List.of(always("0"))),
                        expectedTimeline("J2", "noemer", List.of(always("-28"))),
                        expectedTimeline("J2", "afgekapt", List.of(always("0")))));
    }

    @Test void both_divisions_with_their_deposits_and_an_unbounded_target_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(4 * 3 * 2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_B, List.of(DELER, VERHOUDING, NOEMER, AFGEKAPT), caseB()));
    }

    // ---------------- C: a time-dependent parameter and the calendar ----------------

    static final String EUR = unit("EUR", "Munt", "EUR", 1, 1), DG = unit("dg", "Tijd", "dg", 1, 1),
            JR = unit("jr", "Tijd", "jr", 1, 1), WK = unit("wk", "Tijd", "wk", 1, 1);

    /** The time unit {@code abbreviation} of system Tijd as the denominator of a rate, its own canonical unit. */
    static String per(String abbreviation) {
        return "{\"node\":\"Unit\",\"abbreviation\":\"" + abbreviation + "\",\"system\":\"Tijd\",\"canonical\":\""
                + abbreviation + "\",\"factorToCanonical\":{\"num\":1,\"den\":1},\"exponent\":-1}";
    }

    static final String EUROS = numberType(false, 2, EUR), EUR_PER_YEAR = numberType(false, 2, EUR + "," + per("jr"));

    static String rentevoet(String granularity) {
        return "{\"node\":\"Parameter\",\"name\":\"rentevoet\",\"datatype\":" + numberType(false, 2, per("jr"))
                + ",\"timeline\":{\"node\":\"TimelineFacet\",\"granularity\":\"" + granularity + "\",\"count\":1}}";
    }

    static final String MODEL_C = TmsDocuments.modelWith(List.of(objectType("Afdracht", List.of(
                    TmsDocuments.attribute("afdrachtbedrag", numberType(true, 0, EUR)),
                    timeAttribute("dagrente", EUR_PER_YEAR, "day"), TmsDocuments.attribute("verzuimrente", EUROS),
                    timeAttribute("tarief", EUR_PER_YEAR, "day"), timeAttribute("korting", EUR_PER_YEAR, "day"),
                    timeAttribute("nettotarief", EUR_PER_YEAR, "day"), TmsDocuments.attribute("rente", EUROS)),
                    List.of(timeCharacteristic("rentedragend", "day")))),
            List.of(), List.of(rentevoet("month")),
            List.of(unitSystem("Munt", List.of(EUR)), unitSystem("Tijd", List.of(DG, JR))));

    static final String DAGRENTE = computingRule("dagrente", "BEREKENING", "Afdracht", "dagrente",
            binary("times", parameterRead("rentevoet"), ownRead("afdrachtbedrag", "Afdracht"), EUR_PER_YEAR), "null");

    /** "het totaal van zijn {@code attribute}", gedurende {@code filter} where it is not "null", of {@code type}. */
    static String total(String attribute, String objectType, String filter, String type) {
        return "{\"node\":\"Aggregation\",\"kind\":\"total\",\"arguments\":[" + ownRead(attribute, objectType)
                + "],\"elementType\":\"" + objectType + "\"" + (filter.equals("null") ? "" : ",\"filter\":" + filter)
                + ",\"type\":" + type + ",\"source\":{\"snippet\":\"het totaal van zijn " + attribute + "\"}}";
    }

    static final String VERZUIMRENTE = computingRule("verzuimrente", "BEREKENING", "Afdracht", "verzuimrente",
            rounding(total("dagrente", "Afdracht", check("rentedragend", false), EUROS), "halfAwayFromZero", 2, EUROS),
            "null");
    static final String NETTOTARIEF = computingRule("nettotarief", "BEREKENING", "Afdracht", "nettotarief",
            binary("reducedBy", ownRead("tarief", "Afdracht"), ownRead("korting", "Afdracht"), EUR_PER_YEAR), "null");
    static final String RENTE = computingRule("rente", "BEREKENING", "Afdracht", "rente",
            rounding(total("nettotarief", "Afdracht", "null", EUROS), "halfAwayFromZero", 2, EUROS), "null");

    /** The rentevoet as the case gives it: 0,04 until 2026-02-01 and 0,05 from then, both bounds open. */
    static final String RENTEVOET = "{\"timeline\":[" + openTo("2026-02-01", "0.04") + "," + openFrom("2026-02-01", "0.05")
            + "]}";

    /** A case of {@code instances} and {@code parameters}, each a case value by name, on {@code asOfDate}. */
    static JsonNode caseWith(String asOfDate, List<String> instances, Map<String, String> parameters,
                             List<String> expectations) throws Exception {
        List<String> p = new ArrayList<>();
        parameters.forEach((name, v) -> p.add("\"" + name + "\":" + v));
        return JSON.readTree("{\"format\":\"uchar-case-v0\",\"asOfDate\":" + (asOfDate == null ? "null" : "\"" + asOfDate
                + "\"") + ",\"input\":{\"instances\":[" + String.join(",", instances) + "],\"parameters\":{"
                + String.join(",", p) + "}},\"expected\":[" + String.join(",", expectations) + "]}");
    }

    static JsonNode caseC() throws Exception {
        List<String> dagrente = List.of(openTo("2026-02-01", "400"), openFrom("2026-02-01", "500"));
        return caseWith(null, List.of(
                        instance("A1", "Afdracht", Map.of("afdrachtbedrag", numberValue("10000"), "tarief",
                                timeline(List.of(segment("2028-02-11", "2028-04-05", "500")))), List.of(),
                                Map.of("rentedragend", List.of(period("2028-03-01", "2028-03-11"))), Map.of()),
                        instance("A2", "Afdracht", Map.of("afdrachtbedrag", numberValue("10000")), List.of(),
                                Map.of("rentedragend", List.of(period("2025-11-16", "2026-03-13"))), Map.of())),
                Map.of("rentevoet", RENTEVOET),
                List.of(expectedTimeline("A1", "dagrente", dagrente), value("A1", "verzuimrente", "13.66"),
                        expectedTimeline("A1", "nettotarief", List.of(segment("2028-02-11", "2028-04-05", "500"))),
                        value("A1", "rente", "73.77"),
                        expectedTimeline("A2", "dagrente", dagrente), value("A2", "verzuimrente", "139.18"),
                        expectedTimeline("A2", "nettotarief", List.of()), value("A2", "rente", "0"), consistent()));
    }

    @Test void a_time_dependent_parameter_and_the_calendar_clipped_and_not_in_every_order_on_both_branches()
            throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(4 * 3 * 2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_C, List.of(DAGRENTE, VERZUIMRENTE, NETTOTARIEF, RENTE), caseC()));
    }

    static final String MODEL_BUCKET = TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                    timeAttribute("tarief", numberType(true, 0, EUR + "," + per("dg")), "month"),
                    timeAttribute("weektarief", numberType(true, 0, EUR + "," + per("wk")), "month"),
                    TmsDocuments.attribute("maandtotaal", numberType(true, 0, EUR)),
                    TmsDocuments.attribute("weektotaal", numberType(false, 2, EUR))), List.of())),
            List.of(), List.of(), List.of(unitSystem("Munt", List.of(EUR)), unitSystem("Tijd", List.of(DG, WK))));
    static final String MAANDTOTAAL = computingRule("maandtotaal", "BEREKENING", "Jutter", "maandtotaal",
            total("tarief", "Jutter", "null", numberType(true, 0, EUR)), "null");
    static final String WEEKTOTAAL = computingRule("weektotaal", "BEREKENING", "Jutter", "weektotaal",
            rounding(total("weektarief", "Jutter", "null", numberType(true, 0, EUR)), "halfAwayFromZero", 2, EUROS),
            "null");

    static JsonNode bucketCase() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-02-15", List.of(instance("J1", "Jutter",
                        Map.of("tarief", numberValue("10"), "weektarief", numberValue("7")), List.of(), Map.of(), Map.of())),
                List.of(value("J1", "maandtotaal", "290"), value("J1", "weektotaal", "29"), consistent()));
    }

    /** The asOfDate bucket of a month grid, February 2024, holds 29 days: 10 €/dg is 290 €, 7 €/wk is 29 €. */
    @Test void the_calendar_on_the_route_of_the_bucket_scalar_in_every_order_on_both_branches() throws Exception {
        assertEquals(TmsPeriodArithmeticWitnessTest.gespeeld(2), TmsPeriodArithmeticWitnessTest.inEveryOrder(
                MODEL_BUCKET, List.of(MAANDTOTAAL, WEEKTOTAAL), bucketCase()));
    }

    static final String MODEL_SHARE = TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                    TmsDocuments.attribute("aandeel", numberType(true, 0, TmsDocuments.PERCENT)),
                    timeAttribute("vangst", numberType(true, 0, EUR), "day"),
                    timeAttribute("vangstheffing", EUROS, "day")), List.of())),
            List.of(), List.of(), List.of(unitSystem("Munt", List.of(EUR))));
    static final String VANGSTHEFFING = computingRule("vangstheffing", "BEREKENING", "Jutter", "vangstheffing",
            binary("percentageOf", ownRead("aandeel", "Jutter"), ownRead("vangst", "Jutter"), numberType(false, 0, EUR)),
            "null");

    static JsonNode shareCase() throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("aandeel", numberValue("10"),
                        "vangst", timeline(List.of(segment("2024-01-10", "2024-02-01", "200")))), List.of(), Map.of(), Map.of())),
                List.of(expectedTimeline("J1", "vangstheffing", List.of(openTo("2024-01-10", "0"),
                        segment("2024-01-10", "2024-02-01", "20"), openFrom("2024-02-01", "0"))), consistent()));
    }

    /** "De vangstheffing (€) … als zijn aandeel (%) van zijn vangst (€)": a percentage takes the unit of its right operand
     *  (RUNTIME-SEMANTICS ch. 2.6), 10% of 200 € on [2024-01-10, 2024-02-01) and of a leeg vangst 0 elsewhere. */
    @Test void a_percentage_per_day_takes_the_unit_of_its_right_operand() throws Exception {
        BothBranches.green(document(MODEL_SHARE, List.of(VANGSTHEFFING)), shareCase(), "10% van 200 €, 20 €");
    }

    // ---------------- D: what the branch refuses by name ----------------

    @Test void every_form_outside_what_the_branch_carries_is_refused_by_name_on_both_branches() throws Exception {
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter",
                Map.of("strandloon", STRANDLOON), List.of(), Map.of(), Map.of())), List.of(consistent()));
        // a power, below another operator and as the whole value
        String power = binary("power", ownRead("stormtoeslag", "Jutter"), number("2"));
        BothBranches.refused(document(MODEL_A, List.of(computingRule("loonproduct", "BEREKENING", "Jutter", "loonproduct",
                        binary("times", ownRead("strandloon", "Jutter"), power), "null"))), c,
                "regel: loonproduct — deze operand van het rekenen per periode van ch. 3.4/3.5 wordt niet uitgevoerd ('power')",
                "DROOLS-303");
        BothBranches.refused(document(MODEL_A, List.of(computingRule("loonproduct", "BEREKENING", "Jutter", "loonproduct",
                        power, "null"))), c,
                "regel: loonproduct — een waarde van het tijdsafhankelijke doel 'loonproduct' van Jutter die geen"
                        + " tijdsevenredig deel, geen dagentelling en geen rekenen per periode met een operator van ch. 1.2"
                        + " behalve de macht of met een som is ('power')", "DROOLS-105");
        // a product whose right operand takes a factor into the left factor of its class: wk = 7 dg
        String week = unit("wk", "Tijd", "dg", 7, 1);
        String units = TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                        timeAttribute("weken", numberType(true, 0, week), "day"),
                        timeAttribute("dagtarief", numberType(true, 0, EUR + "," + per("dg")), "day"),
                        timeAttribute("loon", numberType(true, 0, EUR), "day")), List.of())),
                List.of(), List.of(), List.of(unitSystem("Munt", List.of(EUR)), unitSystem("Tijd", List.of(DG, week))));
        BothBranches.refused(document(units, List.of(computingRule("loon", "BEREKENING", "Jutter", "loon",
                        binary("times", ownRead("weken", "Jutter"), ownRead("dagtarief", "Jutter")), "null"))), c,
                "regel: loon — het rekenen per periode van ch. 3.4/3.5 herrekent de operand 'zijn dagtarief' van EUR/dg"
                        + " naar wk", "DROOLS-306");
        // the per-element fold over a rate the calendar integrates
        String fold = TmsDocuments.modelWith(List.of(
                        objectType("Jutter", List.of(TmsDocuments.attribute("totaalrente", numberType(true, 0, EUR))), List.of()),
                        objectType("Vondst", List.of(timeAttribute("rente", numberType(true, 0, EUR + "," + per("jr")), "day")),
                                List.of())),
                List.of(factTypeOneToMany("aanmelding", "jutter", "Jutter", "vondst", "Vondst", "vondsten")), List.of(),
                List.of(unitSystem("Munt", List.of(EUR)), unitSystem("Tijd", List.of(DG, JR))));
        BothBranches.refused(document(fold, List.of(computingRule("totaalrente", "BEREKENING", "Jutter", "totaalrente",
                        aggregation("total", ownRead("rente", "Vondst"), "Vondst", sourceRole("aanmelding", "vondst"), "null",
                                "null", numberType(true, 0, EUR), "het totaal van de rente van alle vondsten van de Jutter"),
                        "null"))), c,
                "over 'rente' met het tarief EUR/jr op een raster van DAG: de noemer is niet de eenheid van het raster",
                "DROOLS-228");
        // a declared factor between the two families, which would stand in for the calendar (WF-009): jr = 365 dg
        String yearOfDays = unit("jr", "Tijd", "dg", 365, 1);
        String declared = TmsDocuments.modelWith(List.of(objectType("Afdracht", List.of(
                        timeAttribute("dagrente", numberType(false, 2, EUR + ",{\"node\":\"Unit\",\"abbreviation\":\"jr\","
                                + "\"system\":\"Tijd\",\"canonical\":\"dg\",\"factorToCanonical\":{\"num\":365,\"den\":1},"
                                + "\"exponent\":-1}"), "day"),
                        TmsDocuments.attribute("rente", EUROS)), List.of())),
                List.of(), List.of(), List.of(unitSystem("Munt", List.of(EUR)), unitSystem("Tijd", List.of(DG, yearOfDays))));
        BothBranches.refused(document(declared, List.of(computingRule("rente", "BEREKENING", "Afdracht", "rente",
                        rounding(total("dagrente", "Afdracht", "null", EUROS), "halfAwayFromZero", 2, EUROS), "null"))),
                TmsTimelineDocuments.caseDocument(null, List.of(instance("A1", "Afdracht", Map.of(), List.of(), Map.of(),
                        Map.of())), List.of(consistent())),
                "over 'dagrente' met het tarief EUR/jr op een raster van DAG: de noemer is niet de eenheid van het raster",
                "DROOLS-228");
    }

    // ---------------- E: the faults of these forms ----------------

    @Test void an_unbound_time_dependent_parameter_faults_with_ef_014() throws Exception {
        JsonNode c = caseWith(null, List.of(instance("A1", "Afdracht", Map.of("afdrachtbedrag", numberValue("10000")),
                List.of(), Map.of(), Map.of())), Map.of(), List.of(consistent()));
        assertEquals("ComputeFault: fout in de regel \"dagrente\": parameter \"rentevoet\" is unbound in this case"
                + " (binding is fail-fast, ch. 5.7)", TmsFaultWitnessTest.playBoth(document(MODEL_C, List.of(DAGRENTE)), c));
    }

    /** The case reader reads a parameter as the reference interpreter's case loader does: a time-dependent parameter
     *  takes a timeline and a scalar one a value, either in the other form is a defective case, and a segment of the
     *  timeline that the declared decimals do not hold is refused at the input (EF-004). */
    @Test void the_case_reader_holds_a_time_dependent_parameter_to_its_timeline() throws Exception {
        Path ir = document(MODEL_C, List.of(DAGRENTE));
        List<String> a1 = List.of(instance("A1", "Afdracht", Map.of("afdrachtbedrag", numberValue("10000")), List.of(),
                Map.of(), Map.of()));
        assertEquals("IllegalArgumentException: parameter rentevoet: a scalar value on a granular parameter (it takes a"
                + " timeline)", TmsFaultWitnessTest.playBoth(ir, caseWith(null, a1, Map.of("rentevoet", numberValue("0.04")),
                List.of(consistent()))));
        String scalar = TmsDocuments.modelWith(List.of(objectType("Afdracht", List.of(
                        TmsDocuments.attribute("afdrachtbedrag", numberType(true, 0, EUR))), List.of())), List.of(),
                List.of(TmsDocuments.parameter("rentevoet", numberType(false, 2, null))),
                List.of(unitSystem("Munt", List.of(EUR))));
        assertEquals("IllegalArgumentException: parameter rentevoet: a timeline value on a non-granular parameter",
                TmsFaultWitnessTest.playBoth(document(scalar, List.of()), caseWith(null, a1,
                        Map.of("rentevoet", RENTEVOET), List.of(consistent()))));
        String fine = "{\"timeline\":[" + openTo("2026-02-01", "0.045") + "]}";
        assertEquals("InputPrecisionFault: invoer parameter rentevoet = 0.045 past niet in 2 decimalen: een casewaarde"
                + " waarvan de declaratie de exacte waarde niet kan houden, wordt bij de invoer geweigerd (RUNTIME-SEMANTICS"
                + " ch. 2.4, 5.7)",
                TmsFaultWitnessTest.playBoth(ir, caseWith(null, a1, Map.of("rentevoet", fine), List.of(consistent()))));
    }

    /** 100/−28 is −25/7, which does not terminate: the unbounded form holds it exactly in the reference interpreter,
     *  and a segment of this branch holds a decimal, so the session has no answer of the branch: a limit, refused by
     *  name with its code (DROOLS-312) on both branches, and no fault of the document. */
    @Test void a_segment_of_the_unbounded_form_that_does_not_terminate_is_refused_as_a_limit() throws Exception {
        String quotient = computingRule("verhouding", "BEREKENING", "Jutter", "verhouding",
                binary("dividedBy", ownRead("strandloon", "Jutter"), ownRead("noemer", "Jutter"), numberType(false, 0, null)),
                "null");
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("strandloon",
                STRANDLOON, "stormtoeslag", STORMTOESLAG), List.of(), Map.of(), Map.of())), List.of(consistent()));
        Path ir = document(MODEL_B, List.of(NOEMER, quotient));
        assertEquals("Excluded: het rekenen per periode van ch. 3.4 schrijft -25/7 op [2024-01-10, 2024-04-03) in de"
                + " tijdlijn van 'verhouding', van de onbegrensde vorm: de waarde eindigt niet, en een segment van deze tak"
                + " houdt een decimaal", TmsFaultWitnessTest.playBoth(ir, c));
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.playCase(ir, c, branch));
            assertEquals(dev.uchar.regelspraak.refusal.DroolsCode.NON_TERMINATING_SEGMENT, e.code(), branch::name);
        }
        // the red path and the readings refuse the session in the same code, since they read the same fixpoint
        Path casePath = java.nio.file.Files.createTempFile("tms-per-dag-grens", ".json");
        casePath.toFile().deleteOnExit();
        java.nio.file.Files.writeString(casePath, c.toString());
        for (DroolsBackend.Branch branch : DroolsBackend.Branch.values()) {
            CaseRunner.RedResult red = CaseRunner.runRed(ir, casePath, branch);
            assertEquals("EXCLUDE DROOLS-312", red.status() + " " + red.droolsCode().id(), branch::name);
            CaseRunner.Excluded e = org.junit.jupiter.api.Assertions.assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.readings(ir, List.of(casePath), branch));
            assertEquals(dev.uchar.regelspraak.refusal.DroolsCode.NON_TERMINATING_SEGMENT, e.code(), branch::name);
        }
    }

    /** Two segments that do not terminate, in two rules, are two limits at the fixpoint: the write-back refuses the
     *  session with the first in the order of the faults, by the name of the form, and does not throw them as two
     *  evaluation faults of the document. */
    @Test void two_limits_at_the_fixpoint_refuse_with_the_first_in_the_order_of_the_faults() throws Exception {
        String model = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                timeAttribute("strandloon", WHOLE, "day"), timeAttribute("stormtoeslag", WHOLE, "day"),
                timeAttribute("noemer", WHOLE, "day"), timeAttribute("verhouding", numberType(false, 0, null), "day"),
                timeAttribute("derde", numberType(false, 0, null), "day")), List.of())), List.of());
        String quotient = computingRule("verhouding", "BEREKENING", "Jutter", "verhouding",
                binary("dividedBy", ownRead("strandloon", "Jutter"), ownRead("noemer", "Jutter"), numberType(false, 0, null)),
                "null");
        String third = computingRule("derde", "BEREKENING", "Jutter", "derde",
                binary("dividedBy", ownRead("strandloon", "Jutter"), number("3"), numberType(false, 0, null)), "null");
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("strandloon",
                STRANDLOON, "stormtoeslag", STORMTOESLAG), List.of(), Map.of(), Map.of())), List.of(consistent()));
        assertEquals("Excluded: het rekenen per periode van ch. 3.4 schrijft 100/3 op [2024-01-10, 2024-07-05) in de"
                + " tijdlijn van 'derde', van de onbegrensde vorm: de waarde eindigt niet, en een segment van deze tak"
                + " houdt een decimaal", TmsFaultWitnessTest.playBoth(document(model, List.of(NOEMER, quotient, third)), c));
    }

    /** 100/8 is 25/2, which a whole target does not hold (RUNTIME-SEMANTICS ch. 2.4 point 1, EF-006). */
    @Test void a_quotient_per_period_that_does_not_fit_faults_at_the_write_boundary() throws Exception {
        String whole = computingRule("deler", "BEREKENING", "Jutter", "deler",
                binary("dividedBy", ownRead("strandloon", "Jutter"), ownRead("korting", "Jutter")), "null");
        JsonNode c = TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("strandloon",
                STRANDLOON, "korting", timeline(List.of(segment("2024-04-03", "2024-06-01", "8")))), List.of(), Map.of(),
                Map.of())), List.of(consistent()));
        assertEquals("WriteBoundaryFault: fout in de regel \"deler\": J1.deler: de waarde 25/2 past niet in de gedeclareerde"
                + " 0 decimalen — een expliciete afronding is verplicht (RUNTIME-SEMANTICS ch. 2.4)",
                TmsFaultWitnessTest.playBoth(document(MODEL_B, List.of(whole)), c));
    }

    // ---------------- helpers ----------------

    static Path document(String model, List<String> rules) throws Exception {
        return TmsTimelineDocuments.document(model, rules, List.of());
    }

    /** Writes the documents of A, B, C and the bucket route with their cases under {@code args[0]}, for the reference
     *  interpreter and the ASP backend. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        TmsTimelineWitnessTest.write(out.resolve("a-maal-verminderd-percentage"),
                TmsTimelineDocuments.ir(MODEL_A, List.of(PRODUCT, NETTO, HEFFING, JAARPRODUCT), List.of()), caseA());
        TmsTimelineWitnessTest.write(out.resolve("a-percentage-eenheid"),
                TmsTimelineDocuments.ir(MODEL_SHARE, List.of(VANGSTHEFFING), List.of()), shareCase());
        TmsTimelineWitnessTest.write(out.resolve("b-delingen"),
                TmsTimelineDocuments.ir(MODEL_B, List.of(DELER, VERHOUDING, NOEMER, AFGEKAPT), List.of()), caseB());
        TmsTimelineWitnessTest.write(out.resolve("c-parameter-kalender"),
                TmsTimelineDocuments.ir(MODEL_C, List.of(DAGRENTE, VERZUIMRENTE, NETTOTARIEF, RENTE), List.of()), caseC());
        TmsTimelineWitnessTest.write(out.resolve("c-emmer-kalender"),
                TmsTimelineDocuments.ir(MODEL_BUCKET, List.of(MAANDTOTAAL, WEEKTOTAAL), List.of()), bucketCase());
    }
}
