package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.DATUM;
import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
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
 * <p><b>Measured</b> 2026-10-10 on the documents of {@link #main}, each with the case of {@link #zaak}: the verifier
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
 * DRL-text branch deposit an inconsistency through a companion of the anchor rule. Which answer holds is a question of
 * the norm.
 *
 * <p><b>Red</b> on the claim of the card, 331b1179: both playing documents were refused, "tms-transpiler SKIP:
 * startpuntbepaling: boekjaar — een StartPointDetermination: een variabel startpunt draagt de truth-maintenance-tak
 * nog niet; geen golden met cases heeft er een, en de caselezer legt geen tijdlijn op zo'n raster", and every manifest
 * of a refused form carried that refusal beside the form's own.
 */
class TmsStartpuntWitnessTest {

    /** The determination's own refusal on the code before this witness; no manifest may carry it any more. */
    private static final String OUDE_WEIGERING = "tms-transpiler SKIP: startpuntbepaling: boekjaar";

    /** The year grid of the named timeline 'boekjaar', whose periods start per object at the startdatum. */
    private static final String BOEKJAARFACET = "{\"node\":\"TimelineFacet\",\"granularity\":\"year\",\"count\":1,"
            + "\"timelineName\":\"boekjaar\"}";

    /** A Jutter with an aanmelddatum, the anchor startdatum, two members on the calendar's month grid and one on its
     *  year grid, a whole jaarloon and the characteristic actief per month; with {@code opRaster} also the vergoeding on
     *  the grid of 'boekjaar'. */
    static String model(boolean opRaster) {
        List<String> attributen = new ArrayList<>(List.of(TmsDocumenten.attribuut("aanmelddatum", DATUM),
                TmsDocumenten.attribuut("startdatum", DATUM),
                TmsTijdlijnDocumenten.tijdattribuut("maandloon", GEHEEL, "month"),
                TmsTijdlijnDocumenten.tijdattribuut("maanddeel", GEHEEL, "month"),
                TmsTijdlijnDocumenten.tijdattribuut("jaarbedrag", GEHEEL, "year"),
                TmsDocumenten.attribuut("jaarloon", GEHEEL)));
        if (opRaster)
            attributen.add("{\"node\":\"Attribute\",\"name\":\"vergoeding\",\"datatype\":" + GEHEEL + ",\"timeline\":"
                    + BOEKJAARFACET + "}");
        return TmsDocumenten.model(List.of(TmsTijdlijnDocumenten.objecttype("Jutter", attributen,
                        List.of(TmsTijdlijnDocumenten.tijdkenmerk("actief", "month")))), List.of())
                // the model builder declares no named timeline; these documents need the one with a variable start
                .replace("\"timelines\":[]", "\"timelines\":[{\"node\":\"Timeline\",\"name\":\"boekjaar\","
                        + "\"variableStart\":true,\"granularity\":\"year\",\"count\":1}]");
    }

    /** The document of {@code model} with {@code regels}, the anchor rule first, and the determination of 'boekjaar'. */
    static String document(String model, List<String> regels) {
        List<String> alle = new ArrayList<>();
        alle.add(ANKERREGEL);
        alle.addAll(regels);
        return TmsTijdlijnDocumenten.ir(model, alle, List.of())
                // the document builder writes no start point determination; these documents need one
                .replace("\"startPointDeterminations\":[]", "\"startPointDeterminations\":[{\"node\":"
                        + "\"StartPointDetermination\",\"timeline\":\"boekjaar\",\"objectType\":\"Jutter\","
                        + "\"member\":\"startdatum\"}]");
    }

    /** "De startdatum van een Jutter moet gesteld worden op zijn aanmelddatum": the ordinary rule that writes the
     *  anchor, as the parity zone's slices write theirs. */
    private static final String ANKERREGEL = rekenregel("startdatum van een jutter", "GELIJKSTELLING", "Jutter",
            "startdatum", lezing("aanmelddatum", "Jutter"), "null");

    /** Two Jutters, each with an aanmelddatum, which the anchor rule writes into the startdatum. Every anchor is
     *  filled: what a leeg anchor deposits is a question of the norm this witness does not pin (see the class). */
    static JsonNode zaak() throws Exception {
        return TmsTijdlijnDocumenten.zaak("2024-06-01", List.of(jutter("J1", "2024-04-01"), jutter("J2", "2024-11-15")),
                List.of(startdatum("J1", "2024-04-01"), startdatum("J2", "2024-11-15"), TmsDocumenten.consistent()));
    }

    private static String jutter(String label, String aanmelddatum) {
        return TmsTijdlijnDocumenten.instantie(label, "Jutter", Map.of("aanmelddatum", "{\"date\":\"" + aanmelddatum
                + "\"}"), List.of(), Map.of(), Map.of());
    }

    private static String startdatum(String label, String datum) {
        return "{\"kind\":\"attributeValue\",\"label\":\"" + label + "\",\"attribute\":\"startdatum\",\"value\":"
                + "{\"date\":\"" + datum + "\"}}";
    }

    /** One form over the grid of 'boekjaar': its rules beside the anchor rule, and the one refusal of its manifest. */
    record Rij(String naam, List<String> regels, String zin) {}

    private static String geweigerd(String regel, String wat) {
        return "tms-transpiler SKIP: regel: " + regel + " — " + wat + " over het attribuut 'vergoeding' van Jutter op een"
                + " tijdlijn met een variabel startpunt: de truth-maintenance-tak draagt deze vorm nog niet; geen golden"
                + " met cases leest of schrijft er een, en de caselezer legt geen tijdlijn op zo'n raster";
    }

    static List<Rij> rijen() {
        String meerDan10 = TmsDocumenten.vergelijking("greater", lezing("vergoeding", "Jutter"), TmsDocumenten.getal("10"));
        String actief = TmsDocumenten.check("actief", false);
        String heel = TmsGeheelPeriodeWitnessTest.heleVergelijking("greater", lezing("vergoeding", "Jutter"),
                TmsDocumenten.getal("0"), TmsGeheelPeriodeWitnessTest.heel(true, false),
                "zijn vergoeding gedurende het gehele jaar groter is dan 0");
        return List.of(
                // the attribute on the grid as the target of an ordinary calculation
                new Rij("doel", List.of(rekenregel("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                        TmsDocumenten.getal("10"), "null")), geweigerd("vergoeding", "een tijdsafhankelijk doel")),
                // "het totaal van zijn vergoeding"
                new Rij("totaal", List.of(rekenregel("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsTijdlijnDocumenten.eigenTotaal("vergoeding", "Jutter", "null"), "null")),
                        geweigerd("jaarloon", "een totaal over de eigen tijdlijn ('het totaal van zijn vergoeding')")),
                // "het totaal van zijn maandloon gedurende de tijd dat zijn vergoeding groter is dan 10"
                new Rij("knip", List.of(rekenregel("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsTijdlijnDocumenten.eigenTotaal("maandloon", "Jutter", meerDan10), "null")),
                        geweigerd("jaarloon", "een totaal over de eigen tijdlijn ('het totaal van zijn maandloon')")),
                // a pro-rata part over the vergoeding onto the maanddeel, and one over the jaarbedrag onto the vergoeding
                new Rij("naar rato, basis", List.of(rekenregel("maanddeel", "BEREKENING", "Jutter", "maanddeel",
                                TmsTijdlijnDocumenten.tijdsevenredig("vergoeding", "Jutter", actief, "month"), "null")),
                        geweigerd("maanddeel", "een tijdsevenredig deel ('het tijdsevenredig deel per month van zijn"
                                + " vergoeding')")),
                new Rij("naar rato, doel", List.of(rekenregel("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                                TmsTijdlijnDocumenten.tijdsevenredig("jaarbedrag", "Jutter", actief, "year"), "null")),
                        geweigerd("vergoeding", "een tijdsafhankelijk doel")),
                // "het aantal dagen in het jaar dat hij actief is" onto the vergoeding
                new Rij("dagentelling", List.of(rekenregel("vergoeding", "BEREKENING", "Jutter", "vergoeding",
                                TmsTijdlijnDocumenten.dagentelling(actief, "year"), "null")),
                        geweigerd("vergoeding", "een tijdsafhankelijk doel")),
                // "… indien zijn vergoeding gedurende het gehele jaar groter is dan 0"
                new Rij("gedurende het gehele", List.of(rekenregel("jaarloon", "BEREKENING", "Jutter", "jaarloon",
                                TmsDocumenten.getal("1"), heel)),
                        geweigerd("jaarloon", "een Comparison met wholePeriod ('zijn vergoeding gedurende het gehele"
                                + " jaar groter is dan 0')")));
    }

    private static Path tijdelijk(String ir) throws Exception {
        Path p = Files.writeString(Files.createTempFile("tms-startpunt", ".json"), ir);
        p.toFile().deleteOnExit();
        return p;
    }

    @Test void een_startpuntbepaling_zonder_tijdlijn_op_haar_raster_speelt() throws Exception {
        // the parity zone's shape: the determination and its timeline, and no member on that grid
        assertNull(CaseRunner.speelCase(tijdelijk(document(model(false), List.of())), zaak(),
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
    }

    @Test void een_lid_op_het_raster_dat_geen_vorm_leest_of_schrijft_speelt() throws Exception {
        assertNull(CaseRunner.speelCase(tijdelijk(document(model(true), List.of())), zaak(),
                DroolsBackend.Branch.TRUTH_MAINTENANCE));
    }

    @Test void elke_vorm_die_het_raster_leest_of_schrijft_wordt_met_naam_geweigerd() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Rij r : rijen()) {
            uitkomsten.add(r.naam() + ": " + String.join(" | ",
                    TmsTijdlijnWeigeringWitnessTest.weigeringen(tijdelijk(document(model(true), r.regels())))));
            verwacht.add(r.naam() + ": " + r.zin());
        }
        assertEquals(verwacht, uitkomsten);
        for (String u : uitkomsten) assertEquals(-1, u.indexOf(OUDE_WEIGERING), u);
    }

    /** Writes the two playing documents and the document of every refused form, each with the case of {@link #zaak},
     *  under {@code args[0]}, for the verifier, ref, clingo and the other two branches. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("zonder-lid"), document(model(false), List.of()), zaak());
        TmsTijdlijnWitnessTest.schrijf(uit.resolve("ongelezen-lid"), document(model(true), List.of()), zaak());
        for (Rij r : rijen())
            TmsTijdlijnWitnessTest.schrijf(uit.resolve(r.naam().replace(", ", "-").replace(' ', '-')),
                    document(model(true), r.regels()), zaak());
    }
}
