package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two consequences of the rules of a fold that no session of the corpus or the suite fired on {@code DRL_TMS} at step
 * 3a of card drl-teksttak-op-truth-maintenance (lens on step 3a): the deposit of a filter ({@code Filterdeponering})
 * and the fault of a member ({@code Lidfout}). The documents are those of the witnesses of the exec-model branch that
 * make these facts, {@link TmsFilterdeponeringWitnessTest} and part E of {@link TmsInvoervormWitnessTest}, here fired
 * through both branches: each case gets the same verdict, and the dump of its session ({@link Sessiedump}) is the same
 * in its input, in what {@code DroolsBackend.lees} gives and in the facts at the fixpoint ({@link Takdumps}), and holds
 * the fact. The third path the lens named, a member's place
 * over two steps, the corpus fires on {@code DRL_TMS} (measured at fix round 3a: in five goldens a contribution or the
 * fault of a filter with such a place, the same as on the exec-model branch).
 *
 * <p><b>Red by mutation</b>, measured at fix round 3a on {@code e22775af} in a copy whose rendering puts the fold's
 * subject in place of the member, in the deposit and in the fault of a member: the first test failed on "de dumps van
 * de twee takken, zaak 1", {@code feit Filterdeponering(vouw=String("drukte: vouw 1"), onderwerp=J1, element=V2)} on the
 * exec-model branch against {@code element=J1} on {@code DRL_TMS}; the second on "de dumps van de twee takken, de zaak
 * van deel E", the three faults on V2, V5 and V6 against {@code element=J1}, {@code J4} and {@code J4}, with the same
 * verdict on both branches, which only the dump tells apart. With the fault of a member rendered without its place, the
 * second failed on the verdict: MVEL could not construct the fact.
 */
class DrlTmsVouwpadWitnessTest {

    @Test void de_deponering_van_een_filter_is_op_beide_takken_gelijk() throws Exception {
        Path ir = TmsDocumenten.document(TmsFilterdeponeringWitnessTest.MODEL,
                List.of(TmsFilterdeponeringWitnessTest.DEELNEMERS, TmsFilterdeponeringWitnessTest.ZWARE,
                        TmsFilterdeponeringWitnessTest.DRUKTE, TmsFilterdeponeringWitnessTest.ZORGVULDIG));
        List<String> metFeit = new ArrayList<>();
        int n = 0;
        for (JsonNode zaak : List.of(TmsFilterdeponeringWitnessTest.zaak1(), TmsFilterdeponeringWitnessTest.zaak2(),
                TmsFilterdeponeringWitnessTest.zaak3())) {
            n++;
            if (Takdumps.gelijk(ir, zaak, "zaak " + n).contains("feit Filterdeponering(")) metFeit.add("zaak " + n);
        }
        assertEquals(List.of("zaak 1", "zaak 2"), metFeit, "de zaken die een filterdeponering maken");
    }

    @Test void de_fout_van_een_lid_is_op_beide_takken_gelijk() throws Exception {
        Path ir = TmsTijdlijnDocumenten.document(TmsInvoervormWitnessTest.MODEL_E, TmsInvoervormWitnessTest.REGELS_E,
                List.of());
        List<String> instanties = new ArrayList<>(List.of(TmsInvoervormWitnessTest.jutter("J1", List.of("V1", "V2")),
                TmsInvoervormWitnessTest.jutter("J4", List.of("V6", "V5")),
                TmsInvoervormWitnessTest.vondst("V1", "8", Map.of("jaartarief", TmsInvoervormWitnessTest.JAARTARIEF_12),
                        TmsInvoervormWitnessTest.BEWAARD_Q1),
                TmsInvoervormWitnessTest.vondst("V2", "8", Map.of("tarief", TmsDocumenten.getalwaarde("5")),
                        Map.of()),
                TmsInvoervormWitnessTest.vondst("V5", "9", Map.of("tarief", TmsDocumenten.getalwaarde("4")),
                        Map.of()),
                TmsInvoervormWitnessTest.vondst("V6", "9", Map.of("tarief", TmsDocumenten.getalwaarde("6")),
                        Map.of())));
        instanties.addAll(TmsInvoervormWitnessTest.spelendeE());
        JsonNode zaak = TmsTijdlijnDocumenten.zaak(TmsInvoervormWitnessTest.DATUM, instanties, List.of(consistent()));
        String dump = Takdumps.gelijk(ir, zaak, "de zaak van deel E");
        assertEquals(3, Stream.of(dump.split("\n")).filter(r -> r.startsWith("feit Lidfout(")).count(),
                () -> "drie lidfouten, op V2, V5 en V6:\n" + dump);
    }
}
