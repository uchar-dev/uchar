package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.eigenTotaal;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.segment;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdlijn;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The timeline forms the truth-maintenance branch refuses by name (RUNTIME-SEMANTICS ch. 3), each in a document of its
 * own with the sentence the build pins: what no golden with cases asks, and what the branch cannot compute as the
 * reference interpreter does with the runtime's helpers it has.
 *
 * <p><b>The comparison clip, "gelijk aan" and "ongelijk aan"</b> (ch. 3.6): "het totaal van zijn vergoeding gedurende
 * de tijd dat zijn controle gelijk is aan zijn grens", and the worked example of ch. 3.6 verbatim, "… gedurende de tijd
 * dat zijn controle ongelijk is aan 1000". The runtime's {@code rsTijdConditie} reads an empty side, a gap in the
 * compared timeline or a leeg bound, as no period, where ch. 3.6 applies the comparison of ch. 1.2 per period: two
 * leeg sides are equal, and exactly one leeg side is unequal. Vergoeding 10 per month January–June 2024, controle 5
 * in January alone: with the grens leeg, "gelijk aan" holds February–June, 5 × 10 = 50, where that helper gives 0;
 * with the grens 5 both give 10. Vergoeding 100 January–June 2024, controle 1000 in January alone: "ongelijk aan 1000"
 * holds February–June, 5 × 100 = 500, where that helper gives 0. <b>Measured</b> (2026-10-09, the documents of
 * {@link #main}): the verifier passes both; the reference interpreter's runner passes them with 50 and 10, and 500;
 * the ASP runner refuses both, "TRANSLATION FAULT: equality in a per-segment clip comparison is not translated (no
 * corpus case exercises it)", so no golden has either; the exec-model and the DRL-text branch answer "FAIL case.json:
 * attribuut wachtsom: verwacht 50, werkelijk 0" and "… verwacht 500, werkelijk 0". <b>Red</b> on 24c8688b, before
 * "gelijk aan" was refused: "but was: &lt;[knip-gelijk: gebouwd, knip-ongelijk: tms-transpiler SKIP: regel: ongelijke
 * wachtsom — … met 'ongelijk aan' in de knip ('notEqual'): de truth-maintenance-tak draagt deze vorm nog niet; de
 * rekenhulp rsTijdConditie leest een gat in de vergeleken tijdlijn als geen periode, …&gt;": the branch built the
 * equality and answered 0 for J1, as the other two branches do.
 */
class TmsTijdlijnWeigeringWitnessTest {

    /** One refused form: the map name of its document, the document's model, rules and case, and the sentence. */
    record Rij(String naam, String model, List<String> regels, JsonNode zaak, String zin) {}

    static final String MODEL_KNIP = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            tijdattribuut("vergoeding", GEHEEL, "month"), tijdattribuut("controle", GEHEEL, "month"),
            TmsDocumenten.attribuut("grens", GEHEEL), TmsDocumenten.attribuut("wachtsom", GEHEEL)), List.of())),
            List.of());

    /** The sentence of a comparison clip with {@code op} ("gelijk aan", "ongelijk aan") spelled {@code snippet}. */
    static String knipzin(String regel, String op, String snippet) {
        return "tms-transpiler SKIP: regel: " + regel + " — een totaal over de eigen tijdlijn ('het totaal van zijn"
                + " vergoeding') met '" + op + "' in de knip ('" + snippet + "'): de truth-maintenance-tak draagt deze"
                + " vorm nog niet; de rekenhulp rsTijdConditie leest een lege kant, een gat in de vergeleken tijdlijn of"
                + " een lege grens, als geen periode, waar RUNTIME-SEMANTICS ch. 3.6 en de referentie-interpreter de"
                + " vergelijking van ch. 1.2 per periode toepassen: twee lege kanten zijn gelijk, en precies één lege"
                + " kant is ongelijk; geen golden met cases heeft het";
    }

    static List<Rij> rijen() throws Exception {
        List<Rij> uit = new ArrayList<>();
        String tienTotJuli = tijdlijn(List.of(segment("2024-01-01", "2024-07-01", "10")));
        String vijfInJanuari = tijdlijn(List.of(segment("2024-01-01", "2024-02-01", "5")));
        uit.add(new Rij("knip-gelijk", MODEL_KNIP, List.of(rekenregel("gelijke wachtsom", "BEREKENING", "Jutter",
                "wachtsom", eigenTotaal("vergoeding", "Jutter", vergelijking("equal", lezing("controle", "Jutter"),
                        lezing("grens", "Jutter"))), "null")),
                TmsTijdlijnDocumenten.zaak(null, List.of(
                                instantie("J1", "Jutter", Map.of("vergoeding", tienTotJuli, "controle", vijfInJanuari),
                                        List.of(), Map.of(), Map.of()),
                                instantie("J2", "Jutter", Map.of("vergoeding", tienTotJuli, "controle", vijfInJanuari,
                                        "grens", getalwaarde("5")), List.of(), Map.of(), Map.of())),
                        List.of(waarde("J1", "wachtsom", "50"), waarde("J2", "wachtsom", "10"))),
                knipzin("gelijke wachtsom", "gelijk aan", "equal")));
        uit.add(new Rij("knip-ongelijk", MODEL_KNIP, List.of(rekenregel("ongelijke wachtsom", "BEREKENING", "Jutter",
                "wachtsom", eigenTotaal("vergoeding", "Jutter", vergelijking("notEqual", lezing("controle", "Jutter"),
                        getal("1000"))), "null")),
                TmsTijdlijnDocumenten.zaak(null, List.of(instantie("J1", "Jutter", Map.of(
                                "vergoeding", tijdlijn(List.of(segment("2024-01-01", "2024-07-01", "100"))),
                                "controle", tijdlijn(List.of(segment("2024-01-01", "2024-02-01", "1000")))),
                                List.of(), Map.of(), Map.of())),
                        List.of(waarde("J1", "wachtsom", "500"))),
                knipzin("ongelijke wachtsom", "ongelijk aan", "notEqual")));
        return uit;
    }

    @Test void elke_tijdlijnvorm_die_de_tak_niet_draagt_wordt_geweigerd_met_haar_zin() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (Rij r : rijen()) {
            Path ir = TmsTijdlijnDocumenten.document(r.model(), r.regels(), List.of());
            String uitkomst;
            try {
                DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
                uitkomst = "gebouwd";
            } catch (CaseRunner.Excluded e) {
                uitkomst = e.getMessage();
            }
            uitkomsten.add(r.naam() + ": " + uitkomst);
            verwacht.add(r.naam() + ": " + r.zin());
        }
        assertEquals(verwacht, uitkomsten);
    }

    /** Writes the document of every row, with its case, under {@code args[0]}: a map per row holding {@code ir.json} and
     *  {@code case.json}, for the verifier, ref, clingo and the other two branches. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        for (Rij r : rijen())
            TmsTijdlijnWitnessTest.schrijf(uit.resolve(r.naam()), TmsTijdlijnDocumenten.ir(r.model(), r.regels(),
                    List.of()), r.zaak());
    }
}
