package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.engine.BeanCompiler;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Coverage;
import dev.uchar.regelspraak.transpiler.tms.TmsTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.numberValue;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.ownTotal;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.instance;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.objectType;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.period;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.segment;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeAttribute;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeCharacteristic;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeline;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.timeProportional;
import static dev.uchar.regelspraak.cases.TmsTimelineDocuments.expectedTimeline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The timeline forms the exec-model branch refuses by name (RUNTIME-SEMANTICS ch. 3), each in a document of its
 * own, with every refusal of the document's manifest pinned in the words of the build's exclusion: what no golden with
 * cases asks, and what the branch cannot compute as the reference interpreter does with the runtime's helpers it has.
 * Every document is verifier-clean; its case holds the values computed by hand, which the reference interpreter
 * confirms where it answers.
 *
 * <ul>
 * <li><b>An initialisation of a timeline target</b>: the reference interpreter and the ASP runner refuse it, "a
 * granularity-bucketed builtin call is executed only as the whole value of a rule on a temporal target (ch. 3.7,
 * 3.8)".
 * <li><b>A time window in the condition of a rule with a timeline target</b>, "… vanaf 1 januari 2025", which clips
 * the timeline (ch. 3.10), as golden jutterswet-pachtvenster has it, which the DRL-text branch refuses as well. The
 * reference interpreter and the ASP runner answer 59 in 2025.
 * <li><b>A characteristic on a timeline as the target</b>: both refuse it, "temporal characteristic "borgplichtig":
 * temporal writes are not implemented".
 * <li><b>A rate whose denominator is not the unit of the grid</b>: 10 EUR/jr per month through 2024, in a document
 * that declares jr as twelve mnd, totals 10 EUR (ch. 3.6, the explicit rate operand), rounded towards zero to the 0
 * decimals of the target, since the restatement divides by twelve and the precision gate takes such a total only
 * through a rounding (WF-023); the reference interpreter answers 10, the ASP runner refuses the factor 12, and the
 * DRL-text branch refuses it in the same words as the exec-model branch.
 * <li><b>A rate over a grid whose unit the document does not declare as one unit</b>: the same 10 EUR/mnd through 2024
 * in a document whose only unit system is Munt, the denominator written by its abbreviation alone, and in one where the
 * systems Tijd and Kalender both declare an mnd, the one canonical, the other thirty dg. The reference interpreter
 * refuses both, "the temporal total of a EUR/mnd value: the document declares no mnd unit, so this timeline's grid has
 * no unit to integrate against (ch. 3.6)" and "… the document declares 2 different mnd units, so this timeline's grid
 * has no single unit to integrate against (ch. 3.6)", the ASP runner with the same reason after a head of its own,
 * and the DRL-text branch in the same words as the exec-model branch. The case holds 120, the total where the system
 * Tijd alone declares mnd: the control {@link #a_rate_over_a_declared_grid_unit_plays}, which the reference
 * interpreter and the ASP runner answer as well.
 * <li><b>The total without a clip over a timeline a rule derives</b>: the reference interpreter refuses it, "the
 * temporal total of "maanddeel" over a case-supplied full timeline is not exercised by any corpus case; the
 * asOfDate-bucket convention carries the total", and the ASP runner leaves the total leeg.
 * <li><b>A timeline on a variable start point</b>, laid per object by a StartPointDetermination: the case reader
 * lays no timeline on such a grid, so the case carries none, and the reference interpreter and the ASP runner answer
 * the total over no timeline, 0. The rule is refused; the determination itself refuses nothing
 * ({@code TmsStartPointWitnessTest}).
 * <li><b>A parameter on a timeline</b>, read as one value: the reference interpreter refuses it, "parameter
 * "penningbedrag" is granular; reading a vintage in a rule is ch. 8 scope", and so does the ASP runner.
 * </ul>
 *
 * <p>A dimensioned attribute on a timeline has no row: WF-125 refuses a dimension beside a timeline on one attribute
 * and a read or a write of a dimensioned attribute outside a cell, so the branch's refusals of those forms
 * (DROOLS-278 and DROOLS-275) are reached only by a document the verifier refuses.
 *
 * <p><b>Measured</b> 2026-11-03 on the documents of {@link #main}: the verifier passes all ten. The suite does not
 * run the verifier, which is a program outside the jar. <b>Red</b> on b9f77aa7, the code before timelines were
 * carried: every row on the refusal of the timeline as outside the trial, "… — het tijdsafhankelijke attribuut
 * 'jaardeel' van Jutter: tijdlijnen zijn buiten de proef van de truth-maintenance-tak", "… — een Aggregation total
 * over de eigen tijdlijn in een waarde ('het totaal van zijn vergoeding'): tijdlijnen zijn buiten de proef …",
 * "… startpuntbepaling: boekjaar — een StartPointDetermination: tijdlijnen zijn buiten de proef …", "… — de
 * tijdsafhankelijke parameter 'penningbedrag': tijdlijnen zijn buiten de proef …". <b>Red</b> on 2e09716a, before
 * the grid's unit was asked: "kale-noemer: gebouwd, twee-rastereenheden: gebouwd".
 */
class TmsTimelineRefusalWitnessTest {

    /** One refused form: the map name of its document, the document and its case, and every refusal of the document's
     *  manifest in its order, each in the words of the build's exclusion; the build is excluded on the first. */
    record Row(String name, String ir, JsonNode caseDocument, List<String> sentences) {}

    static final String BORGPLICHTIG = check("borgplichtig", false);

    static final String MODEL_JAARDEEL = TmsDocuments.model(List.of(objectType("Jutter", List.of(
                    TmsDocuments.attribute("score", WHOLE), timeAttribute("jaartarief", WHOLE, "year"),
                    timeAttribute("jaardeel", WHOLE, "year")),
            List.of(timeCharacteristic("borgplichtig", "month"), timeCharacteristic("wachtplichtig", "month")))), List.of());

    static final String RATE_2024 = timeline(List.of(segment("2024-01-01", "2025-01-01", "366")));

    /** "Het jaardeel van een Jutter moet berekend worden als het tijdsevenredig deel per jaar van zijn jaartarief
     *  gedurende de tijd dat hij {@code kenmerk} is", as rule {@code name} of kind {@code category}. */
    static String jaardeel(String name, String category, String characteristic, String condition) {
        return computingRule(name, category, "Jutter", "jaardeel", timeProportional("jaartarief", "Jutter",
                check(characteristic, false), "year"), condition);
    }

    static List<Row> rows() throws Exception {
        List<Row> out = new ArrayList<>();
        // an initialisation of a timeline target
        out.add(new Row("initialisatie", TmsTimelineDocuments.ir(MODEL_JAARDEEL, List.of(
                        jaardeel("jaardeel init", "INITIALISATIE", "borgplichtig", "null")), List.of()),
                TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("jaartarief", RATE_2024),
                                List.of(), Map.of("borgplichtig", List.of(period("2024-03-01", "2025-01-01"))), Map.of())),
                        List.of(expectedTimeline("J1", "jaardeel", List.of(segment("2024-01-01", "2025-01-01",
                                "306"))))),
                List.of("transpiler SKIP: regel: jaardeel init — een initialisatie van het tijdsafhankelijke"
                        + " attribuut 'jaardeel' van Jutter: deze tak draagt deze vorm nog niet; geen"
                        + " golden met cases heeft er een")));
        // a time window in the condition of a rule with a timeline target
        out.add(new Row("periodevenster", TmsTimelineDocuments.ir(MODEL_JAARDEEL, List.of(
                        jaardeel("jaardeel vanaf 2025", "BEREKENING", "borgplichtig", TmsDocuments.window(
                                IrPath.literal("2025-01-01", TmsDocuments.DATE, "1 januari 2025"), "null", false))),
                        List.of()),
                TmsTimelineDocuments.caseDocument("2025-06-01", List.of(instance("J1", "Jutter", Map.of("jaartarief",
                                timeline(List.of(segment("2024-01-01", "2025-01-01", "366"),
                                        segment("2025-01-01", "2026-01-01", "365")))), List.of(),
                                Map.of("borgplichtig", List.of(period("2024-03-01", "2025-03-01"))), Map.of())),
                        List.of(expectedTimeline("J1", "jaardeel", List.of(segment("2025-01-01", "2026-01-01",
                                "59"))))),
                List.of("transpiler SKIP: regel: jaardeel vanaf 2025 — een periodevenster in de voorwaarde van een"
                        + " regel met het tijdsafhankelijke doel 'jaardeel' van Jutter: daar knipt het venster de tijdlijn"
                        + " (RUNTIME-SEMANTICS ch. 3.10), en deze tak draagt deze vorm nog niet")));
        // a characteristic on a timeline as the target
        out.add(new Row("kenmerk-als-doel", TmsTimelineDocuments.ir(MODEL_JAARDEEL, List.of(
                        TmsDocuments.characteristicRule("borgplicht", "Jutter", "borgplichtig",
                                comparison("greater", ownRead("score", "Jutter"), number("5")))), List.of()),
                TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("score", numberValue("10")),
                        List.of(), Map.of(), Map.of())), List.of(TmsDocuments.consistent())),
                List.of("transpiler SKIP: regel: borgplicht — het tijdsafhankelijke kenmerk 'borgplichtig' van"
                        + " Jutter als doel: deze tak draagt deze vorm nog niet; geen golden met cases"
                        + " schrijft er een (RUNTIME-SEMANTICS ch. 3.11)")));
        // a rate whose denominator is not the unit of the grid: 10 EUR/jr over the twelve months of 2024
        out.add(new Row("vreemde-noemer", TmsTimelineDocuments.ir(MODEL_RATE, List.of(WACHTSOM_RATE_ROUNDED),
                        List.of()),
                rateCase("10"),
                List.of("transpiler SKIP: regel: wachtsom — een totaal over de eigen tijdlijn ('het totaal van zijn"
                        + " vergoeding') over 'vergoeding' met het tarief EUR/jr op een raster van MAAND: de noemer"
                        + " is niet de eenheid van het raster, en de tak herrekent hem niet (RUNTIME-SEMANTICS ch. 3.6);"
                        + " geen golden met cases heeft het")));
        // two negative factors and none of the grid: 10 EUR/kg/m per month through 2024
        out.add(new Row("twee-noemers-geen-van-het-raster", TmsTimelineDocuments.ir(rateModel(EUR_PER_KG_PER_M, List.of(
                                TmsDocuments.unitSystem("Munt", List.of(EUR)), TmsDocuments.unitSystem("Tijd", List.of(MND, JR)),
                                TmsDocuments.unitSystem("Maat", List.of(TmsDocuments.unit("kg", "Maat", "kg", 1, 1),
                                        TmsDocuments.unit("m", "Maat", "m", 1, 1))))),
                        List.of(WACHTSOM_RATE), List.of()),
                rateCase("10"),
                List.of("transpiler SKIP: regel: wachtsom — een totaal over de eigen tijdlijn ('het totaal van zijn"
                        + " vergoeding') over 'vergoeding' met het tarief EUR/kg/m op een raster van MAAND: de noemer"
                        + " is niet de eenheid van het raster, en de tak herrekent hem niet (RUNTIME-SEMANTICS ch. 3.6);"
                        + " geen golden met cases heeft het")));
        // a rate over a grid whose unit the document does not declare: no unit system spells mnd at all, or two spell
        // it with different canonical chains
        out.add(new Row("kale-noemer", TmsTimelineDocuments.ir(rateModel(EUR_PER_BARE_MND,
                        List.of(TmsDocuments.unitSystem("Munt", List.of(EUR)))), List.of(WACHTSOM_RATE), List.of()),
                rateCase("120"), List.of(gridUnitSentence("the document declares no mnd unit, so this timeline's grid"
                        + " has no unit to integrate against (ch. 3.6)"))));
        out.add(new Row("twee-rastereenheden", TmsTimelineDocuments.ir(rateModel(EUR_PER_MND, List.of(
                                TmsDocuments.unitSystem("Munt", List.of(EUR)),
                                TmsDocuments.unitSystem("Tijd", List.of(MND, JR)),
                                TmsDocuments.unitSystem("Kalender", List.of(TmsDocuments.unit("dg", "Kalender",
                                        "dg", 1, 1), TmsDocuments.unit("mnd", "Kalender", "dg", 30, 1))))),
                        List.of(WACHTSOM_RATE), List.of()),
                rateCase("120"), List.of(gridUnitSentence("the document declares 2 different mnd units, so this"
                        + " timeline's grid has no single unit to integrate against (ch. 3.6)"))));
        // the total without a clip over a timeline a rule derives
        out.add(new Row("totaal-zonder-knip", TmsTimelineDocuments.ir(TmsDocuments.model(List.of(objectType("Jutter",
                                List.of(timeAttribute("jaartarief", WHOLE, "year"), timeAttribute("maanddeel", WHOLE,
                                        "month"), TmsDocuments.attribute("wachtsom", WHOLE)),
                                List.of(timeCharacteristic("borgplichtig", "month")))), List.of()),
                        List.of(computingRule("maanddeel", "BEREKENING", "Jutter", "maanddeel", timeProportional("jaartarief",
                                        "Jutter", BORGPLICHTIG, "month"), "null"),
                                computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom", ownTotal("maanddeel",
                                        "Jutter", "null"), "null")), List.of()),
                TmsTimelineDocuments.caseDocument("2024-02-15", List.of(instance("J1", "Jutter", Map.of("jaartarief",
                                timeline(List.of(segment("2024-01-01", "2025-01-01", "12")))), List.of(),
                                Map.of("borgplichtig", List.of(period("2024-01-01", "2024-04-01"))), Map.of())),
                        List.of(value("J1", "wachtsom", "36"))),
                List.of("transpiler SKIP: regel: wachtsom — een totaal over de eigen tijdlijn ('het totaal van zijn"
                        + " maanddeel') zonder knip over de tijdlijn van 'maanddeel', die een vorm schrijft: de"
                        + " referentie-interpreter neemt het totaal zonder knip alleen van de scalaire invoer op de periode"
                        + " van de rekendatum en weigert een volle tijdlijn (RUNTIME-SEMANTICS ch. 3.6); deze"
                        + " tak draagt deze vorm nog niet, en geen golden met cases heeft het")));
        // a timeline on a variable start point, which a StartPointDetermination lays per object
        out.add(new Row("variabel-startpunt", TmsTimelineDocuments.ir(MODEL_START_POINT, List.of(computingRule("peilsom",
                        "BEREKENING", "Schouwveld", "peilsom", ownTotal("waterstand", "Schouwveld",
                                check("drooggevallen", false)), "null")), List.of())
                        // the document builder writes no start point determination; this row needs one
                        .replace("\"startPointDeterminations\":[]", "\"startPointDeterminations\":[{\"node\":"
                                + "\"StartPointDetermination\",\"timeline\":\"boekjaar\",\"objectType\":\"Schouwveld\","
                                + "\"member\":\"meetdatum\"}]"),
                TmsTimelineDocuments.caseDocument(null, List.of(instance("S1", "Schouwveld", Map.of("meetdatum",
                                "{\"date\":\"2024-04-01\"}"), List.of(), Map.of(), Map.of())),
                        List.of(value("S1", "peilsom", "0"))),
                List.of("transpiler SKIP: regel: peilsom — een totaal over de eigen tijdlijn ('het totaal van zijn"
                                + " waterstand') over het attribuut 'waterstand' van Schouwveld op een tijdlijn met een"
                                + " variabel startpunt: deze tak draagt deze vorm nog niet; geen golden met"
                                + " cases leest of schrijft er een, en de caselezer legt geen tijdlijn op zo'n raster")));
        // a parameter on a timeline, read as one value
        JsonNode parameterCase = TmsTimelineDocuments.caseDocument("2024-06-01", List.of(instance("J1", "Jutter", Map.of(),
                List.of(), Map.of(), Map.of())), List.of(value("J1", "opbrengst", "7")));
        ((com.fasterxml.jackson.databind.node.ObjectNode) parameterCase.get("input")).set("parameters",
                TmsDocuments.JSON.readTree("{\"penningbedrag\":" + timeline(List.of(segment("2024-01-01",
                        "2025-01-01", "7"))) + "}"));
        out.add(new Row("tijdsafhankelijke-parameter", TmsTimelineDocuments.ir(TmsDocuments.modelWith(List.of(
                                objectType("Jutter", List.of(TmsDocuments.attribute("opbrengst", WHOLE)), List.of())),
                        List.of(), List.of("{\"node\":\"Parameter\",\"name\":\"penningbedrag\",\"datatype\":" + WHOLE
                                + ",\"timeline\":" + TmsTimelineDocuments.facet("year", null) + "}"), List.of()),
                        List.of(computingRule("opbrengst", "BEREKENING", "Jutter", "opbrengst",
                                TmsDocuments.parameterRead("penningbedrag"), "null")), List.of()),
                parameterCase,
                List.of("transpiler SKIP: regel: opbrengst — de tijdsafhankelijke parameter 'penningbedrag': deze"
                        + " tak draagt deze vorm nog niet; de goldens met cases die er een lezen, doen"
                        + " dat in een TemporalRead, die de lader voor alle takken weigert")));
        return out;
    }

    // ---------------- the units of the row of a foreign denominator ----------------

    static final String EUR = TmsDocuments.unit("EUR", "Munt", "EUR", 1, 1);
    static final String MND = "{\"node\":\"Unit\",\"abbreviation\":\"mnd\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":1,\"den\":1},\"spellings\":[\"maand\"]}";
    static final String JR = "{\"node\":\"Unit\",\"abbreviation\":\"jr\",\"system\":\"Tijd\",\"canonical\":\"mnd\","
            + "\"factorToCanonical\":{\"num\":12,\"den\":1},\"spellings\":[\"jaar\"]}";
    /** EUR per jr, a rate whose denominator the document declares as twelve of the month grid's unit. */
    static final String EUR_PER_JR = EUR + "," + denominator(TmsDocuments.unit("jr", "Tijd", "mnd", 12, 1));

    /** EUR per mnd, the month grid's own unit as the system Tijd declares it. */
    static final String EUR_PER_MND = EUR + "," + denominator(TmsDocuments.unit("mnd", "Tijd", "mnd", 1, 1));
    /** EUR per mnd with the denominator written by its abbreviation alone, as a calendar unit may be. */
    static final String EUR_PER_BARE_MND = EUR + "," + denominator(TmsDocuments.unit("mnd"));
    static final String EUR_PER_KG_PER_M = EUR + "," + denominator(TmsDocuments.unit("kg", "Maat", "kg", 1, 1)) + ","
            + denominator(TmsDocuments.unit("m", "Maat", "m", 1, 1));

    /** A Unit node as the factor of a denominator. */
    private static String denominator(String unit) {
        return unit.substring(0, unit.length() - 1) + ",\"exponent\":-1}";
    }

    static final String MODEL_RATE = rateModel(EUR_PER_JR, List.of(TmsDocuments.unitSystem("Munt",
            List.of(EUR)), TmsDocuments.unitSystem("Tijd", List.of(MND, JR))));

    /** A Jutter with a vergoeding on a month grid in the rate unit {@code tarief}, a wachtsom in EUR and the
     *  characteristic wachtplichtig on a timeline, under the unit systems {@code systems}. */
    static String rateModel(String rate, List<String> systems) {
        return TmsDocuments.modelWith(List.of(objectType("Jutter", List.of(
                        "{\"node\":\"Attribute\",\"name\":\"vergoeding\",\"datatype\":" + TmsDocuments.numberType(true, 0,
                                rate) + ",\"timeline\":" + TmsTimelineDocuments.facet("month", null) + "}",
                        TmsDocuments.attribute("wachtsom", TmsDocuments.numberType(true, 0, EUR))),
                List.of(timeCharacteristic("wachtplichtig", "month")))), List.of(), List.of(), systems);
    }

    /** "het totaal van zijn vergoeding gedurende de tijd dat hij wachtplichtig is", in EUR. */
    static final String TOTAL_RATE = "{\"node\":\"Aggregation\",\"kind\":\"total\",\"arguments\":["
            + ownRead("vergoeding", "Jutter") + "],\"elementType\":\"Jutter\",\"filter\":"
            + check("wachtplichtig", false) + ",\"type\":" + TmsDocuments.numberType(true, 0, EUR)
            + ",\"source\":{\"snippet\":\"het totaal van zijn vergoeding\"}}";

    /** "De wachtsom van een Jutter moet berekend worden als het totaal van zijn vergoeding gedurende de tijd dat hij
     *  wachtplichtig is", in EUR. */
    static final String WACHTSOM_RATE = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom", TOTAL_RATE,
            "null");

    /** The same total "afgerond op 0 decimalen" towards zero: the restatement of EUR/jr into the month grid's unit
     *  divides by twelve, so the total without a rounding has no finite precision bound, and a target of 0 decimals
     *  takes it only through a rounding (WF-023, RUNTIME-SEMANTICS ch. 2.3). */
    static final String WACHTSOM_RATE_ROUNDED = computingRule("wachtsom", "BEREKENING", "Jutter", "wachtsom",
            TmsDocuments.rounding(TOTAL_RATE, "towardZero", 0, TmsDocuments.numberType(true, 0, EUR)), "null");

    /** J1 with a vergoeding of 10 through 2024 and wachtplichtig through 2024, its wachtsom expected {@code expected}. */
    static JsonNode rateCase(String expected) throws Exception {
        return TmsTimelineDocuments.caseDocument(null, List.of(instance("J1", "Jutter", Map.of("vergoeding",
                        timeline(List.of(segment("2024-01-01", "2025-01-01", "10")))), List.of(),
                        Map.of("wachtplichtig", List.of(period("2024-01-01", "2025-01-01"))), Map.of())),
                List.of(value("J1", "wachtsom", expected)));
    }

    /** The refusal of the wachtsom over the rate EUR/mnd where the document does not declare the month grid's unit as
     *  exactly one unit: the form, the member and the reference interpreter's sentence, which ends in {@code reason}. */
    static String gridUnitSentence(String reason) {
        return "transpiler SKIP: regel: wachtsom — een totaal over de eigen tijdlijn ('het totaal van zijn"
                + " vergoeding') over 'vergoeding': the temporal total of a EUR/mnd value: " + reason;
    }

    // ---------------- the model of the row of a variable start point ----------------

    static final String MODEL_START_POINT = TmsDocuments.model(List.of(objectType("Schouwveld", List.of(
                    TmsDocuments.attribute("meetdatum", TmsDocuments.DATE),
                    "{\"node\":\"Attribute\",\"name\":\"waterstand\",\"datatype\":" + WHOLE + ",\"timeline\":{\"node\":"
                            + "\"TimelineFacet\",\"granularity\":\"year\",\"count\":1,\"timelineName\":\"boekjaar\"}}",
                    TmsDocuments.attribute("peilsom", WHOLE)),
            List.of(timeCharacteristic("drooggevallen", "month")))), List.of())
            // the model builder declares no named timeline; this row needs the one its member lies on
            .replace("\"timelines\":[]", "\"timelines\":[{\"node\":\"Timeline\",\"name\":\"boekjaar\","
                    + "\"variableStart\":true,\"granularity\":\"year\",\"count\":1}]");

    /** The control of the rows of the grid's unit: the same rate over a document whose system Tijd declares mnd, the
     *  month grid's unit, builds, and its total integrates twelve months of 10 EUR/mnd to 120 EUR, as the reference
     *  interpreter and the ASP runner answer. */
    @Test void a_rate_over_a_declared_grid_unit_plays() throws Exception {
        Path ir = Files.writeString(Files.createTempFile("tms-rastereenheid", ".json"), TmsTimelineDocuments.ir(
                rateModel(EUR_PER_MND, List.of(TmsDocuments.unitSystem("Munt", List.of(EUR)),
                        TmsDocuments.unitSystem("Tijd", List.of(MND, JR)))), List.of(WACHTSOM_RATE), List.of()));
        ir.toFile().deleteOnExit();
        assertNull(CaseRunner.playCase(ir, rateCase("120"), DroolsBackend.Branch.EXEC_MODEL));
    }

    @Test void every_timeline_form_that_the_branch_does_not_carry_is_refused_with_its_sentence() throws Exception {
        List<String> outcomes = new ArrayList<>(), expected = new ArrayList<>();
        for (Row r : rows()) {
            Path ir = Files.writeString(Files.createTempFile("tms-tijdlijnweigering", ".json"), r.ir());
            ir.toFile().deleteOnExit();
            String built;
            try {
                DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
                built = "gebouwd";
            } catch (CaseRunner.Excluded e) {
                built = e.getMessage();
            }
            List<String> all = refusals(ir);
            outcomes.add(r.name() + ": " + built + (all.size() > 1 ? " | "
                    + String.join(" | ", all.subList(1, all.size())) : ""));
            expected.add(r.name() + ": " + String.join(" | ", r.sentences()));
        }
        assertEquals(expected, outcomes);
    }

    /** Every refusal of the manifest of the document {@code ir}, in its order, in the words of the build's exclusion. */
    static List<String> refusals(Path ir) throws Exception {
        IrLoader.Positioned p = IrLoader.loadWithPositions(TmsDocuments.JSON.readTree(Files.readString(ir)));
        IrLoader.Loaded l = p.loaded();
        ClassLoader cl = BeanCompiler.compile(FactModelGenerator.generate(l.model()));
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            TmsTranspiler t = TmsTranspiler.build(l.model(), cl, l.prioritizedDefaults(), l.roleBindings(), l.lets(),
                    l.sourcePaths(), l.navigationBindings(), l.anchored(), p.positions());
            List<String> out = new ArrayList<>();
            for (Coverage d : t.coverage())
                if (!d.lowered()) out.add("transpiler " + d.verdict() + ": " + d.key()
                        + (d.reason().isEmpty() ? "" : " — " + d.reason()));
            return out;
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    /** Writes the document of every row, with its case, under {@code args[0]}: a map per row holding {@code ir.json} and
     *  {@code case.json}, for the verifier, ref, clingo and the DRL-text branch. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        for (Row r : rows()) TmsTimelineWitnessTest.write(out.resolve(r.name()), r.ir(), r.caseDocument());
    }
}
