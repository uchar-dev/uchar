package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.eigenTotaal;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.kenmerk;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsTijdlijnDocumenten.tijdattribuut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The subject's own temporal total without a clause in a session without an evaluation date (RUNTIME-SEMANTICS ch. 3.6,
 * 8.4): that route takes the asOfDate-bucket scalar, the session's date being its horizon, so without a date it has no
 * bucket to take, and the reference interpreter faults where it evaluates the total, whether the attribute holds a value
 * or not: "the temporal total of "bedrag" needs the evaluation date: the case carries the asOfDate bucket's value". On
 * this branch the fault is a {@code Fout} that counts only where it stands at the fixpoint, like every evaluation fault.
 *
 * <p>The document: "Een Jutter is vrijgesteld indien zijn score kleiner is dan 5" and "De topsom van een Jutter moet
 * berekend worden als het totaal van zijn bedrag, indien hij niet vrijgesteld is", bedrag on a month grid. In a session
 * without a date: J1 (score 3) is vrijgesteld, so the total is not evaluated at the fixpoint and the session plays,
 * topsom leeg; where the topsom's rule fires before the vrijstelling, it evaluates the total on that intermediate state
 * and faults, and the fault goes with the state. J2 (score 10) is not vrijgesteld, and the session faults. With the
 * evaluation date 2024-06-15: J2 with bedrag 7 as the bucket scalar of June 2024 totals 7, J3 (score 10) without a
 * bedrag leeg, and J1, with the same bedrag, stays leeg. Both orders of the two rules; beside them the generated rules in
 * a hundred sampled orders on a J1 of its own, in some of which the fault was inserted and withdrawn.
 *
 * <p><b>Measured</b> (2026-10-09, the documents {@link #main} writes, both orders): the verifier passes them. The
 * reference interpreter's runner passes the session with J1 (2 expectations) and the one with a date (4), and faults on
 * J2 in the sentence above. The ASP runner passes the session with a date and refuses both sessions without one at
 * translation, in the same sentence, also the one with J1, whose total no evaluation reaches: it decides before any
 * condition is read.
 *
 * <p><b>Red</b> on 0e879163, the code before the change: the session with J2 played, topsom leeg, "[vrijstelling,
 * topsom] niet vrijgesteld gespeeld" in both orders, and no sampled order inserted a fault ("but was: &lt;false&gt;").
 */
class TmsTotaalRekendatumWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(objecttype("Jutter", List.of(
            TmsDocumenten.attribuut("score", GEHEEL), tijdattribuut("bedrag", GEHEEL, "month"),
            TmsDocumenten.attribuut("topsom", GEHEEL)), List.of(kenmerk("vrijgesteld")))), List.of());

    static final String VRIJSTELLING = TmsDocumenten.kenmerkregel("vrijstelling", "Jutter", "vrijgesteld",
            vergelijking("less", lezing("score", "Jutter"), getal("5")));
    static final String TOPSOM = rekenregel("topsom", "BEREKENING", "Jutter", "topsom", eigenTotaal("bedrag", "Jutter",
            "null"), check("vrijgesteld", true));

    static final List<String> REGELS = List.of(VRIJSTELLING, TOPSOM);

    static final String ZONDER_DATUM = "Rekenfout: fout in de regel \"topsom\": the temporal total of \"bedrag\" needs"
            + " the evaluation date: the case carries the asOfDate bucket's value";

    static JsonNode vrijgesteld() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(instantie("J1", "Jutter", Map.of("score", getalwaarde("3")),
                List.of(), Map.of(), Map.of())), List.of(waarde("J1", "topsom", null), consistent()));
    }

    static JsonNode nietVrijgesteld() throws Exception {
        return TmsTijdlijnDocumenten.zaak(null, List.of(instantie("J2", "Jutter", Map.of("score", getalwaarde("10")),
                List.of(), Map.of(), Map.of())), List.of(consistent()));
    }

    static JsonNode metDatum() throws Exception {
        return TmsTijdlijnDocumenten.zaak("2024-06-15", List.of(
                        instantie("J1", "Jutter", Map.of("score", getalwaarde("3"), "bedrag", getalwaarde("7")), List.of(),
                                Map.of(), Map.of()),
                        instantie("J2", "Jutter", Map.of("score", getalwaarde("10"), "bedrag", getalwaarde("7")), List.of(),
                                Map.of(), Map.of()),
                        instantie("J3", "Jutter", Map.of("score", getalwaarde("10")), List.of(), Map.of(), Map.of())),
                List.of(waarde("J1", "topsom", null), waarde("J2", "topsom", "7"), waarde("J3", "topsom", null),
                        consistent()));
    }

    @Test void het_totaal_zonder_knip_faalt_zonder_rekendatum_waar_de_evaluatie_het_bereikt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsTijdlijnDocumenten.document(MODEL, volgorde, List.of());
            uitkomsten.add(namen + " vrijgesteld " + TmsFoutWitnessTest.speel(ir, vrijgesteld(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            uitkomsten.add(namen + " niet vrijgesteld " + TmsFoutWitnessTest.speel(ir, nietVrijgesteld(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            uitkomsten.add(namen + " met datum " + TmsFoutWitnessTest.speel(ir, metDatum(),
                    DroolsBackend.Branch.TRUTH_MAINTENANCE));
            verwacht.add(namen + " vrijgesteld gespeeld");
            verwacht.add(namen + " niet vrijgesteld " + ZONDER_DATUM);
            verwacht.add(namen + " met datum gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void de_fout_van_een_tussenstand_wordt_ingetrokken() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(MODEL, REGELS, List.of());
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE);
        int tussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(b.tmsTranspiler().model().getRules(), 20261009L, 100)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("score", "3"), List.of());
            TmsLetWitnessTest.Feiten feiten = new TmsLetWitnessTest.Feiten();
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(j1), sessie -> sessie.addEventListener(feiten)),
                    () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten in volgorde " + namen);
            assertNull(TmsDocumenten.lees(j1, "topsom"), () -> "topsom in volgorde " + namen);
            if (feiten.fouten > 0) tussenstand++;
        }
        assertTrue(tussenstand > 0, "geen volgorde liet het totaal vóór de vrijstelling rekenen");
    }

    /** Writes the document in both orders with its three cases under {@code args[0]}, for the verifier, ref and clingo. */
    public static void main(String[] args) throws Exception {
        Path uit = Path.of(args[0]);
        int n = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            Path map = uit.resolve(String.format("v%03d", n++));
            TmsTijdlijnWitnessTest.schrijf(map, TmsTijdlijnDocumenten.ir(MODEL, volgorde, List.of()), vrijgesteld());
            java.nio.file.Files.move(map.resolve("case.json"), map.resolve("case1-vrijgesteld.json"));
            java.nio.file.Files.writeString(map.resolve("case2-niet-vrijgesteld.json"), TmsDocumenten.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(nietVrijgesteld()));
            java.nio.file.Files.writeString(map.resolve("case3-met-datum.json"), TmsDocumenten.JSON
                    .writerWithDefaultPrettyPrinter().writeValueAsString(metDatum()));
        }
    }
}
