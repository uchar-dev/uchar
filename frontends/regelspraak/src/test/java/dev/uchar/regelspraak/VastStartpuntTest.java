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
class VastStartpuntTest {

    private static final String KOP = """
            Domein Bedrag is van het type Numeriek (geheel getal)
            Objecttype de Vlucht (mv: Vluchten)
            """;

    private static String attribuut(String tijdlijn) {
        return KOP + "    de waarde Bedrag " + tijdlijn + ";\n";
    }

    /** The emitted document without whitespace, as the other emission witnesses read it. */
    private static String wire(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.isOk(), () -> "de bron lost foutloos op: " + m.diagnostics);
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static String facet(String granularity, int count, String vast) {
        return "\"timeline\":{\"node\":\"TimelineFacet\",\"granularity\":\"" + granularity + "\",\"count\":" + count
                + ",\"fixedStart\":\"" + vast + "\"";
    }

    private static void emitteert(String bron, String verwacht) {
        String ir = wire(bron);
        assertTrue(ir.contains(verwacht), () -> "verwacht " + verwacht + " in:\n" + ir);
    }

    /** A source that names no day is refused at resolution, by section and clause, and no document is written. */
    private static void weigert(String bron, String sectie, String fragment) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.errors().stream().anyMatch(d -> d.message().contains(sectie) && d.message().contains(fragment)),
                () -> "verwacht een ERROR met " + sectie + " en '" + fragment + "', kreeg: " + m.diagnostics);
        assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m), "een geweigerde bron wordt niet geschreven");
    }

    // ---------------- the date form, §13.3.6.3 ----------------

    @Test void week_startend_op_een_datum() {
        emitteert(attribuut("voor elke week startend op 5-1-2026"), facet("week", 1, "2026-01-05"));
    }

    @Test void meervoudige_periode_startend_op_een_datum() {
        emitteert(attribuut("voor elke 5 maanden startend op 1-4-2026"), facet("month", 5, "2026-04-01"));
    }

    @Test void een_datum_op_een_raster_van_een_maand_wordt_zijn_dag_maand() {
        // "elke 1 maanden" is a month grid of one unit, whose start point is the day of the month the date names
        emitteert(attribuut("voor elke 1 maanden startend op 15-3-2026"), facet("month", 1, "--01-15"));
    }

    @Test void een_datum_met_een_tijdstip_wordt_geweigerd() {
        weigert(attribuut("voor elke week startend op 5-1-2026 12:00:00.000"), "§13.3.6.3", "draagt een tijdstip");
    }

    @Test void een_datum_die_niet_bestaat_wordt_geweigerd() {
        weigert(attribuut("voor elke week startend op 30-2-2026"), "§13.3.6.3", "noemt geen dag van de kalender");
    }

    // ---------------- the day-of forms, §13.3.6.4–.6 ----------------

    @Test void maand_startend_op_een_dag() {
        emitteert(attribuut("voor elke maand startend op dag 15 van de maand"), facet("month", 1, "--01-15"));
        emitteert(attribuut("voor elke maand startend op dag 31 van de maand"), facet("month", 1, "--01-31"));
        weigert(attribuut("voor elke maand startend op dag 32 van de maand"), "§13.3.6.4", "noemt geen dag van de maand");
    }

    @Test void kwartaal_startend_op_een_dag() {
        emitteert(attribuut("voor elk kwartaal startend op dag 1 van maand 2, 5, 8 en 11"), facet("quarter", 1, "--02-01"));
        emitteert(attribuut("voor elk kwartaal startend op dag 10 van maand 1, 4, 7 en 10"), facet("quarter", 1, "--01-10"));
        // day 31 is not in February: the earliest month of the cycle that has it is May
        emitteert(attribuut("voor elk kwartaal startend op dag 31 van maand 2, 5, 8 en 11"), facet("quarter", 1, "--05-31"));
    }

    @Test void kwartaalgroep_die_geen_kwartaalreeks_is_wordt_geweigerd() {
        // the grammar admits any four numbers; the source language admits three groups
        weigert(attribuut("voor elk kwartaal startend op dag 1 van maand 1, 5, 8 en 11"), "§13.3.6.5",
                "noemt geen reeks kwartaalmaanden");
    }

    @Test void jaar_startend_op_een_dag_van_een_maand() {
        emitteert(attribuut("voor elk jaar startend op dag 15 van maand 10"), facet("year", 1, "--10-15"));
        emitteert(attribuut("voor elk jaar startend op dag 15 van maand 1"), facet("year", 1, "--01-15"));
        emitteert(attribuut("voor elk jaar startend op dag 29 van maand 2"), facet("year", 1, "--02-29"));
        weigert(attribuut("voor elk jaar startend op dag 30 van maand 2"), "§13.3.6.6", "noemt een dag die maand 2 niet heeft");
        weigert(attribuut("voor elk jaar startend op dag 1 van maand 13"), "§13.3.6.6", "noemt geen maand");
    }

    // ---------------- the other positions ----------------

    @Test void kenmerk_met_een_vast_startpunt() {
        emitteert(KOP + "    is duurzaam kenmerk (bijvoeglijk) voor elke week startend op 5-1-2026;\n",
                facet("week", 1, "2026-01-05"));
    }

    @Test void parameter_met_een_vast_startpunt() {
        emitteert(KOP + "Parameter het weektarief : Numeriek (geheel getal) voor elke week startend op 5-1-2026\n",
                facet("week", 1, "2026-01-05"));
    }

    /** A parameter's facet has count 1 while multi-period grids are open, and the model's parameter has no count to
     *  write: refused in WF-005's words rather than written as one period. Measured before this card: refused for its
     *  start point only; through a named timeline the count was dropped without a word. */
    @Test void parameter_over_meerdere_perioden_wordt_geweigerd() {
        weigert(KOP + "Parameter het bloktarief : Numeriek (geheel getal) voor elke 5 maanden startend op 1-4-2026\n",
                "parameter 'bloktarief'", "has count 5 > 1: a parameter's timeline is one period");
    }

    @Test void benoemde_tijdlijn_draagt_haar_startpunt_en_het_facet_neemt_het_over() {
        String ir = wire("Tijdlijn het boekjaar heeft tijdvakken van een jaar startend op dag 1 van maand 7;\n" + KOP
                + "    de waarde Bedrag voor elke boekjaar;\n");
        assertTrue(ir.contains("\"node\":\"Timeline\",\"name\":\"hetboekjaar\",\"variableStart\":false,"
                + "\"granularity\":\"year\",\"count\":1,\"fixedStart\":\"--07-01\"}"), ir);
        assertTrue(ir.contains(facet("year", 1, "--07-01") + ",\"timelineName\":\"boekjaar\"}"), ir);
        emitteert("Tijdlijn de schoft heeft tijdvakken van een week startend op 5-1-2026;\n" + KOP
                + "    de waarde Bedrag voor elke schoft;\n", facet("week", 1, "2026-01-05") + ",\"timelineName\":\"schoft\"}");
        emitteert("Tijdlijn het blok heeft tijdvakken van 5 maanden startend op 1-4-2026;\n" + KOP
                + "    de waarde Bedrag voor elke blok;\n", facet("month", 5, "2026-04-01") + ",\"timelineName\":\"blok\"}");
    }

    // ---------------- the lossless omission: the calendar start point ----------------

    /** Emitted through the real CLI path; the bytes of the document it wrote. */
    private static String emissie(String bron) throws Exception {
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

    @Test void het_kalenderstartpunt_valt_bewust_en_verliesvrij_weg() throws Exception {
        assertEquals(emissie(attribuut("voor elke maand")),
                emissie(attribuut("voor elke maand startend op dag 1 van de maand")));
        assertEquals(emissie(attribuut("voor elk kwartaal")),
                emissie(attribuut("voor elk kwartaal startend op dag 1 van maand 1, 4, 7 en 10")));
        assertEquals(emissie(attribuut("voor elk jaar")),
                emissie(attribuut("voor elk jaar startend op dag 1 van maand 1")));
    }

    /** WF-032: a start point determination anchors a timeline declared with a VARIABLE start (§9.9), and the verifier
     *  refuses one for a fixed-start timeline. Measured on the frontend before this: a WARNING, the document written,
     *  and {@code WF-032 FAIL /rules/startPointDeterminations/0: start point for fixed-start timeline 'penningjaar'}. */
    @Test void een_startpuntbepaling_voor_een_tijdlijn_met_vast_startpunt_wordt_geweigerd() {
        String bepaling = """

            Objecttype de Jutter (mv: Jutters) (bezield)
                de aanvangsdatum Datum in dagen;

            Regel startpunt penningjaar
                geldig altijd
                    Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.
            """;
        RegelSpraakModel vast = Resolver.resolve(
                "Tijdlijn het penningjaar heeft tijdvakken van een jaar startend op dag 1 van maand 1;\n" + bepaling);
        assertEquals(List.of("ERROR startpuntbepaling (regel 'startpunt penningjaar'): start point for fixed-start "
                        + "timeline 'penningjaar'"),
                vast.diagnostics.stream().map(d -> d.severity() + " " + d.message()).toList());
        RegelSpraakModel variabel = Resolver.resolve(
                "Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;\n" + bepaling);
        assertTrue(variabel.errors().isEmpty(), () -> "onverwachte fout: " + variabel.errors());
    }
}
