package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.DATE;
import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A variable start point (RUNTIME-SEMANTICS ch. 3.2): a {@code StartPointDetermination} names the date member whose
 * value anchors, per object, the periods of a timeline declared with {@code variableStart}. The determination is a
 * declaration and fires nothing; its anchor member is an ordinary date attribute that an ordinary rule writes. The
 * reference interpreter faults only where it lays a period of such a grid ("a variable-start grid is laid per object
 * from the anchor its start-point determination binds, which this interpreter does not execute (ch. 3.2)"), and its
 * case loader refuses a case value on one. So a document that declares a variable start point and reads or writes no
 * timeline on its grid plays, the shape of the parity zone's slices; a form that reads or writes a timeline on that
 * grid is refused by name where it asks for the member: an attribute on the grid as the target of a rule, the total of
 * its timeline, a clip that compares it, a pro-rata part over it or onto it, a day count onto it, and "gedurende het
 * gehele" over it.
 *
 * <p><b>Measured</b> 2026-10-10 on the documents of {@link #main}, each with the case of {@link #caseDocument}: the verifier
 * passes all nine. The two that play are answered by the reference interpreter, the ASP runner, the exec-model and the
 * DRL-text branch as here: J1's startdatum 2024-04-01, J2's 2024-11-15, consistent. Of the seven refused forms the
 * reference interpreter refuses the target ("temporal attribute "vergoeding" as a target: temporal writes are not
 * implemented") and both pro-rata parts ("pro-rata onto or from a grid that does not lie on the calendar is fenced
 * (RUNTIME-SEMANTICS ch. 3.2)"), and answers the total, the clip, the day count and the whole-period test, since this
 * case gives no timeline on the grid and none of them then lays a period of it; the ASP runner refuses the target, the
 * total ("a variable-start grid is laid per object from the anchor its start-point determination binds, which this
 * interpreter does not execute (ch. 3.2)") and both pro-rata parts, and answers the other three. This branch refuses all
 * seven by name, as the decision of the card asks.
 *
 * <p><b>Not pinned</b>: a leeg anchor. Ch. 3.2 says that the anchor rule's own §6.8 error surfaces on the §9.5 channel
 * then; the reference interpreter and the ASP runner answer consistent, as this branch does, and the exec-model and the
 * DRL-text branch deposit an inconsistency through a companion of the anchor rule. This branch keeps the answer of the
 * two: where no form reads or writes a timeline on the grid, nothing evaluates the anchor (call-by-need, ch. 10.6), and
 * every form that does is refused by name, so a leeg anchor has nothing here to surface through.
 *
 * <p><b>Red</b> on the claim of the card, 331b1179: both playing documents were refused, "tms-transpiler SKIP:
 * startpuntbepaling: boekjaar — een StartPointDetermination: een variabel startpunt draagt de truth-maintenance-tak
 * nog niet; geen golden met cases heeft er een, en de caselezer legt geen tijdlijn op zo'n raster", and every manifest
 * of a refused form carried that refusal beside the form's own.
 */
class TmsStartPointWitnessTest {

    /** The determination's own refusal on the code before this witness; no manifest may carry it any more. */
    private static final String OLD_REFUSAL = "transpiler SKIP: startpuntbepaling: boekjaar";

    /** The year grid of the named timeline 'boekjaar', whose periods start per object at the startdatum. */
    private static final String BOEKJAAR_FACET = "{\"node\":\"TimelineFacet\",\"granularity\":\"year\",\"count\":1,"
            + "\"timelineName\":\"boekjaar\"}";

    /** A Jutter with an aanmelddatum, the anchor startdatum, two members on the calendar's month grid and one on its
     *  year grid, a whole jaarloon and the characteristic actief per month; with {@code onGrid} also the vergoeding on
     *  the grid of 'boekjaar'. */
    static String model(boolean onGrid) {
        List<String> attributes = new ArrayList<>(List.of(TmsDocuments.attribuut("aanmelddatum", DATE),
                TmsDocuments.attribuut("startdatum", DATE),
                TmsTimelineDocuments.timeAttribute("maandloon", WHOLE, "month"),
                TmsTimelineDocuments.timeAttribute("maanddeel", WHOLE, "month"),
                TmsTimelineDocuments.timeAttribute("jaarbedrag", WHOLE, "year"),
                TmsDocuments.attribuut("jaarloon", WHOLE)));
        if (onGrid)
            attributes.add("{\"node\":\"Attribute\",\"name\":\"vergoeding\",\"datatype\":" + WHOLE + ",\"timeline\":"
                    + BOEKJAAR_FACET + "}");
        return TmsDocuments.model(List.of(TmsTimelineDocuments.objecttype("Jutter", attributes,
                        List.of(TmsTimelineDocuments.timeCharacteristic("actief", "month")))), List.of())
                // the model builder declares no named timeline; these documents need the one with a variable start
                .replace("\"timelines\":[]", "\"timelines\":[{\"node\":\"Timeline\",\"name\":\"boekjaar\","
                        + "\"variableStart\":true,\"granularity\":\"year\",\"count\":1}]");
    }

    /** The document of {@code model} with {@code rules}, the anchor rule first, and the determination of 'boekjaar'. */
    static String document(String model, List<String> rules) {
        List<String> all = new ArrayList<>();
        all.add(ANCHOR_RULE);
        all.addAll(rules);
        return TmsTimelineDocuments.ir(model, all, List.of())
                // the document builder writes no start point determination; these documents need one
                .replace("\"startPointDeterminations\":[]", "\"startPointDeterminations\":[{\"node\":"
                        + "\"StartPointDetermination\",\"timeline\":\"boekjaar\",\"objectType\":\"Jutter\","
                        + "\"member\":\"startdatum\"}]");
    }

    /** "De startdatum van een Jutter moet gesteld worden op zijn aanmelddatum": the ordinary rule that writes the
     *  anchor, as the parity zone's slices write theirs. */
    private static final String ANCHOR_RULE = computingRule("startdatum van een jutter", "GELIJKSTELLING", "Jutter",
            "startdatum", ownRead("aanmelddatum", "Jutter"), "null");

    /** Two Jutters, each with an aanmelddatum, which the anchor rule writes into the startdatum. Every anchor is
     *  filled: a leeg anchor is not pinned here (see the class). */
    static JsonNode caseDocument() throws Exception {
        return TmsTimelineDocuments.caseDocument("2024-06-01", List.of(jutter("J1", "2024-04-01"), jutter("J2", "2024-11-15")),
                List.of(startDate("J1", "2024-04-01"), startDate("J2", "2024-11-15"), TmsDocuments.consistent()));
    }

    private static String jutter(String label, String aanmelddatum) {
        return TmsTimelineDocuments.instance(label, "Jutter", Map.of("aanmelddatum", "{\"date\":\"" + aanmelddatum
                + "\"}"), List.of(), Map.of(), Map.of());
    }

    private static String startDate(String label, String date) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"startdatum\",\"value\":"
                + "{\"date\":\"" + date + "\"}}";
    }

    /** One form over the grid of 'boekjaar': its rules beside the anchor rule, and the one refusal of its manifest. */
    record Row(String naam, List<String> rules, String sentence) {}

    private static String refused(String regel, String description) {
        return "transpiler SKIP: regel: " + regel + " — " + description + " over het attribuut 'vergoeding' van Jutter op een"
                + " tijdlijn met een variabel startpunt: deze tak draagt deze vorm nog niet; geen golden"
                + " met cases leest of schrijft er een, en de caselezer legt geen tijdlijn op zo'n raster";
    }

    static List<Row> rows() {
        String moreThan10 = TmsDocuments.comparison("greater", ownRead("vergoeding", "Jutter"), TmsDocuments.number("10"));
        String actief = TmsDocuments.check("actief", false);
        String whole = TmsWholePeriodWitnessTest.wholeComparison("greater", ownRead("vergoeding", "Jutter"),
                TmsDocuments.number("0"), TmsWholePeriodWitnessTest.whole(true, false),
                "zijn vergoeding gedurende het gehele jaar groter is dan 0");
        return List.of(
                // the attribute on the grid as the target of an ordinary calculation
                new Row("doel", List.of(computingRule("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                        TmsDocuments.number("10"), "null")), refused("vergoeding", "een tijdsafhankelijk doel")),
                // "het totaal van zijn vergoeding"
                new Row("totaal", List.of(computingRule("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsTimelineDocuments.ownTotal("vergoeding", "Jutter", "null"), "null")),
                        refused("jaarloon", "een totaal over de eigen tijdlijn ('het totaal van zijn vergoeding')")),
                // "het totaal van zijn maandloon gedurende de tijd dat zijn vergoeding groter is dan 10"
                new Row("knip", List.of(computingRule("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsTimelineDocuments.ownTotal("maandloon", "Jutter", moreThan10), "null")),
                        refused("jaarloon", "een totaal over de eigen tijdlijn ('het totaal van zijn maandloon')")),
                // a pro-rata part over the vergoeding onto the maanddeel, and one over the jaarbedrag onto the vergoeding
                new Row("naar rato, basis", List.of(computingRule("maanddeel", "BEREKENING", "Jutter", "maanddeel",
                                TmsTimelineDocuments.timeProportional("vergoeding", "Jutter", actief, "month"), "null")),
                        refused("maanddeel", "een tijdsevenredig deel ('het tijdsevenredig deel per month van zijn"
                                + " vergoeding')")),
                new Row("naar rato, doel", List.of(computingRule("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                                TmsTimelineDocuments.timeProportional("jaarbedrag", "Jutter", actief, "year"), "null")),
                        refused("vergoeding", "een tijdsafhankelijk doel")),
                // "het aantal dagen in het jaar dat hij actief is" onto the vergoeding
                new Row("dagentelling", List.of(computingRule("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                                TmsTimelineDocuments.dayCount(actief, "year"), "null")),
                        refused("vergoeding", "een tijdsafhankelijk doel")),
                // "… indien zijn vergoeding gedurende het gehele jaar groter is dan 0"
                new Row("gedurende het gehele", List.of(computingRule("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsDocuments.number("1"), whole)),
                        refused("jaarloon", "een Comparison met wholePeriod ('zijn vergoeding gedurende het gehele"
                                + " jaar groter is dan 0')")));
    }

    private static Path temporary(String ir) throws Exception {
        Path p = Files.writeString(Files.createTempFile("tms-startpunt", ".json"), ir);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test void a_start_point_determination_without_timeline_on_its_grid_plays() throws Exception {
        // the parity zone's shape: the determination and its timeline, and no member on that grid
        assertNull(CaseRunner.playCase(temporary(document(model(false), List.of())), caseDocument(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    @Test void a_member_on_the_grid_that_no_form_reads_or_writes_plays() throws Exception {
        assertNull(CaseRunner.playCase(temporary(document(model(true), List.of())), caseDocument(),
                DroolsBackend.Branch.EXEC_MODEL));
    }

    @Test void every_form_that_reads_or_writes_the_grid_is_refused_by_name() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Row r : rows()) {
            outcomes.add(r.naam() + ": " + String.join(" | ",
                    TmsTimelineRefusalWitnessTest.refusals(temporary(document(model(true), r.rules())))));
            verwacht.add(r.naam() + ": " + r.sentence());
        }
        assertEquals(verwacht, outcomes);
        for (String u : outcomes) assertEquals(-1, u.indexOf(OLD_REFUSAL), u);
    }

    /** Writes the two playing documents and the document of every refused form, each with the case of {@link #caseDocument},
     *  under {@code args[0]}, for the verifier, ref, clingo and the DRL-text branch. */
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        TmsTimelineWitnessTest.write(out.resolve("zonder-lid"), document(model(false), List.of()), caseDocument());
        TmsTimelineWitnessTest.write(out.resolve("ongelezen-lid"), document(model(true), List.of()), caseDocument());
        for (Row r : rows())
            TmsTimelineWitnessTest.write(out.resolve(r.naam().replace(", ", "-").replace(' ', '-')),
                    document(model(true), r.rules()), caseDocument());
    }
}
