package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed start point of a timeline (§13.3.6.3–.7) on the wire: {@code fixedStart} on a {@code Timeline} and on the
 * timeline facet of an attribute, a characteristic or a parameter (IR-SPEC ch. 2.8).
 *
 * <p><b>Measured 2026-09-26 on the commit before this.</b> Every source below that now emits was refused at emission
 * with an ERROR naming its section, because the wire had no field for the start point, and an executor lays a
 * timeline without one on the calendar grid.
 *
 * <p><b>The spelling.</b> A date on a week grid and on a grid of several units, the canonical day-month on a month,
 * quarter or year grid of one unit: {@code mm} the earliest month of the year that has a wall on day {@code dd}
 * itself. The calendar's own start point is written as none, so the three that coincide with it emit byte for byte
 * the timeline written without one. A source that names no day is refused by name at resolution.
 */
class FixedStartPointTest {

    private static final String HEAD = """
            Domein Bedrag is van het type Numeriek (geheel getal)
            Objecttype de Vlucht (mv: Vluchten)
            """;

    private static String attribuut(String tijdlijn) {
        return HEAD + "    de waarde Bedrag " + tijdlijn + ";\n";
    }

    /** The emitted document without whitespace, as the other emission witnesses read it. */
    private static String wire(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.isOk(), () -> "de bron lost foutloos op: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String facet(String granularity, int count, String fixed) {
        return "\"timeline\":{\"node\":\"TimelineFacet\",\"granularity\":\"" + granularity + "\",\"count\":" + count
                + ",\"fixedStart\":\"" + fixed + "\"";
    }

    private static void emits(String bron, String verwacht) {
        String ir = wire(bron);
        assertTrue(ir.contains(verwacht), () -> "verwacht " + verwacht + " in:\n" + ir);
    }

    /** A source that names no day is refused at resolution, by section and clause, and no document is written. */
    private static void refuses(String bron, String section, String fragment) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.errors().stream().anyMatch(d -> d.message().contains(section) && d.message().contains(fragment)),
                () -> "verwacht een ERROR met " + section + " en '" + fragment + "', kreeg: " + m.diagnostics);
        assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m), "een geweigerde bron wordt niet geschreven");
    }

    // ---------------- the date form, §13.3.6.3 ----------------

    @Test void week_starting_on_a_date() {
        emits(attribuut("voor elke week startend op 5-1-2026"), facet("week", 1, "2026-01-05"));
    }

    @Test void multiple_period_starting_on_a_date() {
        emits(attribuut("voor elke 5 maanden startend op 1-4-2026"), facet("month", 5, "2026-04-01"));
    }

    @Test void a_date_on_a_grid_of_a_month_becomes_its_day_month() {
        // "elke 1 maanden" is a month grid of one unit, whose start point is the day of the month the date names
        emits(attribuut("voor elke 1 maanden startend op 15-3-2026"), facet("month", 1, "--01-15"));
    }

    @Test void a_date_with_a_time_of_day_is_refused() {
        refuses(attribuut("voor elke week startend op 5-1-2026 12:00:00.000"), "§13.3.6.3", "draagt een tijdstip");
    }

    @Test void a_date_that_does_not_exist_is_refused() {
        refuses(attribuut("voor elke week startend op 30-2-2026"), "§13.3.6.3", "noemt geen dag van de kalender");
    }

    // ---------------- the day-of forms, §13.3.6.4–.6 ----------------

    @Test void month_starting_on_a_day() {
        emits(attribuut("voor elke maand startend op dag 15 van de maand"), facet("month", 1, "--01-15"));
        emits(attribuut("voor elke maand startend op dag 31 van de maand"), facet("month", 1, "--01-31"));
        refuses(attribuut("voor elke maand startend op dag 32 van de maand"), "§13.3.6.4", "noemt geen dag van de maand");
    }

    @Test void quarter_starting_on_a_day() {
        emits(attribuut("voor elk kwartaal startend op dag 1 van maand 2, 5, 8 en 11"), facet("quarter", 1, "--02-01"));
        emits(attribuut("voor elk kwartaal startend op dag 10 van maand 1, 4, 7 en 10"), facet("quarter", 1, "--01-10"));
        // day 31 is not in February: the earliest month of the cycle that has it is May
        emits(attribuut("voor elk kwartaal startend op dag 31 van maand 2, 5, 8 en 11"), facet("quarter", 1, "--05-31"));
    }

    @Test void a_quarter_group_that_is_no_quarter_sequence_is_refused() {
        // the grammar admits any four numbers; the source language admits three groups
        refuses(attribuut("voor elk kwartaal startend op dag 1 van maand 1, 5, 8 en 11"), "§13.3.6.5",
                "noemt geen reeks kwartaalmaanden");
    }

    @Test void year_starting_on_a_day_of_a_month() {
        emits(attribuut("voor elk jaar startend op dag 15 van maand 10"), facet("year", 1, "--10-15"));
        emits(attribuut("voor elk jaar startend op dag 15 van maand 1"), facet("year", 1, "--01-15"));
        emits(attribuut("voor elk jaar startend op dag 29 van maand 2"), facet("year", 1, "--02-29"));
        refuses(attribuut("voor elk jaar startend op dag 30 van maand 2"), "§13.3.6.6", "noemt een dag die maand 2 niet heeft");
        refuses(attribuut("voor elk jaar startend op dag 1 van maand 13"), "§13.3.6.6", "noemt geen maand");
    }

    // ---------------- the other positions ----------------

    @Test void characteristic_with_a_fixed_start_point() {
        emits(HEAD + "    is duurzaam kenmerk (bijvoeglijk) voor elke week startend op 5-1-2026;\n",
                facet("week", 1, "2026-01-05"));
    }

    @Test void parameter_with_a_fixed_start_point() {
        emits(HEAD + "Parameter het weektarief : Numeriek (geheel getal) voor elke week startend op 5-1-2026\n",
                facet("week", 1, "2026-01-05"));
    }

    /** A parameter's facet has count 1 while multi-period grids are open, and the model's parameter has no count to
     *  write: refused in WF-005's words rather than written as one period. Measured before this card: refused for its
     *  start point only; through a named timeline the count was dropped without a word. */
    @Test void parameter_over_multiple_periods_is_refused() {
        refuses(HEAD + "Parameter het bloktarief : Numeriek (geheel getal) voor elke 5 maanden startend op 1-4-2026\n",
                "parameter 'bloktarief'", "has count 5 > 1: a parameter's timeline is one period");
    }

    /** A named timeline on the calendar grid has no start point, and the facet that names it none either. Measured on
     *  the first build of this card: the adoption looked the start point up by mapping the timelines before taking
     *  the first, and a null start point threw — gate 10 caught it on jutterswet-penningjaar. */
    @Test void named_timeline_on_the_calendar_grid_gives_the_facet_no_start_point() {
        // a named definition writes its start point or "met variabel startpunt"; the calendar's is day 1
        for (String[] definitie : List.of(new String[]{"een jaar startend op dag 1 van maand 1", "year"},
                new String[]{"een maand startend op dag 1 van de maand", "month"},
                new String[]{"een kwartaal startend op dag 1 van maand 1, 4, 7 en 10", "quarter"})) {
            String ir = wire("Tijdlijn het boekjaar heeft tijdvakken van " + definitie[0] + ";\n" + HEAD
                    + "    de waarde Bedrag voor elke boekjaar;\n");
            assertTrue(ir.contains("\"timeline\":{\"node\":\"TimelineFacet\",\"granularity\":\"" + definitie[1]
                    + "\",\"count\":1,\"timelineName\":\"boekjaar\"}"), ir);
            assertFalse(ir.contains("fixedStart"), ir);
        }
    }

    @Test void named_timeline_carries_its_start_point_and_the_facet_takes_it_over() {
        String ir = wire("Tijdlijn het boekjaar heeft tijdvakken van een jaar startend op dag 1 van maand 7;\n" + HEAD
                + "    de waarde Bedrag voor elke boekjaar;\n");
        assertTrue(ir.contains("\"node\":\"Timeline\",\"name\":\"hetboekjaar\",\"variableStart\":false,"
                + "\"granularity\":\"year\",\"count\":1,\"fixedStart\":\"--07-01\"}"), ir);
        assertTrue(ir.contains(facet("year", 1, "--07-01") + ",\"timelineName\":\"boekjaar\"}"), ir);
        emits("Tijdlijn de schoft heeft tijdvakken van een week startend op 5-1-2026;\n" + HEAD
                + "    de waarde Bedrag voor elke schoft;\n", facet("week", 1, "2026-01-05") + ",\"timelineName\":\"schoft\"}");
        emits("Tijdlijn het blok heeft tijdvakken van 5 maanden startend op 1-4-2026;\n" + HEAD
                + "    de waarde Bedrag voor elke blok;\n", facet("month", 5, "2026-04-01") + ",\"timelineName\":\"blok\"}");
    }

    // ---------------- the lossless omission: the calendar start point ----------------

    /** Emitted through the real CLI path; the bytes of the document it wrote. */
    private static String emission(String bron) throws Exception {
        Path dir = Files.createTempDirectory("vaststartpunt");
        Path rgs = dir.resolve("bron.rgs");
        Path out = dir.resolve("ir.json");
        Files.writeString(rgs, bron, StandardCharsets.UTF_8);
        PrintStream oud = System.out;
        int rc;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            rc = Resolver.irModus(new java.util.ArrayList<>(List.of(out.toString(), rgs.toString())));
        } finally {
            System.setOut(oud);
        }
        assertEquals(0, rc, "het kalenderstartpunt emitteert");
        return Files.readString(out, StandardCharsets.UTF_8)
                // the Bron coordinates carry the file name, which differs per temp dir
                .replace(rgs.toString(), "bron.rgs");
    }

    @Test void the_calendar_start_point_falls_away_deliberately_and_losslessly() throws Exception {
        assertEquals(emission(attribuut("voor elke maand")),
                emission(attribuut("voor elke maand startend op dag 1 van de maand")));
        assertEquals(emission(attribuut("voor elk kwartaal")),
                emission(attribuut("voor elk kwartaal startend op dag 1 van maand 1, 4, 7 en 10")));
        assertEquals(emission(attribuut("voor elk jaar")),
                emission(attribuut("voor elk jaar startend op dag 1 van maand 1")));
    }

    /** A multi-period timeline names its start date (§13.3.6.7); written without one, "voor elke 2 maanden" parses
     *  as a reference to a named timeline "2 maanden", and a named timeline nobody declares is refused as well.
     *  Measured before this: the attribute, the characteristic and the parameter were written without any timeline,
     *  as single values, with {@code diagnostics: []}. */
    @Test void a_timeline_without_a_start_date_or_declaration_is_refused() {
        String withoutStart = "een tijdlijn van meer perioden noemt haar startdatum ('voor elke 2 maanden startend op "
                + "<datum>', §13.3.6.7); zonder startdatum leest deze frontend '2 maanden' als de naam van een tijdlijn, "
                + "en die is niet gedeclareerd";
        RegelSpraakModel m = Resolver.resolve(HEAD + """
                    de waarde Bedrag voor elke 2 maanden;
                    is gecharterd kenmerk (bijvoeglijk) voor elke 2 maanden;
                    de toeslag Bedrag voor elke zeejaar;

                Parameter de heffing : Bedrag voor elke 2 maanden
                """);
        assertEquals(List.of("ERROR (line 3): tijdlijn 'voor elke 2 maanden': " + withoutStart,
                        "ERROR (line 4): tijdlijn 'voor elke 2 maanden': " + withoutStart,
                        "ERROR (line 5): tijdlijn 'voor elke zeejaar': de tijdlijn 'zeejaar' is niet gedeclareerd "
                                + "('Tijdlijn zeejaar heeft tijdvakken van …', §13.3.6.10)",
                        "ERROR (line 7): tijdlijn 'voor elke 2 maanden': " + withoutStart),
                m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        emits(attribuut("voor elke 2 maanden startend op 01-01-2024"), facet("month", 2, "2024-01-01"));
    }

    /** WF-032: a start point determination anchors a timeline declared with a VARIABLE start (§9.9), and the verifier
     *  refuses one for a fixed-start timeline. Measured on the frontend before this: a WARNING, the document written,
     *  and {@code WF-032 FAIL /rules/startPointDeterminations/0: start point for fixed-start timeline 'penningjaar'}. */
    @Test void a_start_point_determination_for_a_timeline_with_fixed_start_point_is_refused() {
        String determination = """

            Objecttype de Jutter (mv: Jutters) (bezield)
                de aanvangsdatum Datum in dagen;

            Regel startpunt penningjaar
                geldig altijd
                    Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.
            """;
        RegelSpraakModel fixed = Resolver.resolve(
                "Tijdlijn het penningjaar heeft tijdvakken van een jaar startend op dag 1 van maand 1;\n" + determination);
        assertEquals(List.of("ERROR startpuntbepaling (regel 'startpunt penningjaar'): start point for fixed-start "
                        + "timeline 'penningjaar'"),
                fixed.diagnostics.stream().map(d -> d.severity() + " " + d.message()).toList());
        RegelSpraakModel variable = Resolver.resolve(
                "Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;\n" + determination);
        assertTrue(variable.errors().isEmpty(), () -> "onverwachte fout: " + variable.errors());
    }
}
