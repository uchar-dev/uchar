package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two consequences of the rules of a fold that no session of the corpus or the suite fired on the analysis rendered as
 * DRL text ({@code DRL_TMS} then, the DRL-text branch since the switch-over) at step 3a of card
 * drl-teksttak-op-truth-maintenance (lens on step 3a): the deposit of a filter ({@code FilterDeposit})
 * and the fault of a member ({@code MemberError}). The documents are those of the witnesses of the exec-model branch that
 * make these facts, {@link TmsFilterDepositWitnessTest} and part E of {@link TmsInputFormWitnessTest}, here fired
 * through both branches: each case gets the same verdict, and the dump of its session ({@link SessionDump}) is the same
 * in its input, in what {@code DroolsBackend.lees} gives and in the facts at the fixpoint ({@link BranchDumps}), and holds
 * the fact. The third path the lens named, a member's place over two steps, the corpus fires on the DRL-text branch
 * (measured at fix round 3a: in five goldens a contribution or the fault of a filter with such a place, the same as on
 * the exec-model branch).
 *
 * <p><b>Red by mutation</b>, measured at fix round 3a on {@code e22775af} in a copy whose rendering puts the fold's
 * subject in place of the member, in the deposit and in the fault of a member: the first test failed on "de dumps van
 * de twee takken, zaak 1", {@code feit Filterdeponering(vouw=String("drukte: vouw 1"), onderwerp=J1, element=V2)} on the
 * exec-model branch against {@code element=J1} on {@code DRL_TMS}; the second on "de dumps van de twee takken, de zaak
 * van deel E", the three faults on V2, V5 and V6 against {@code element=J1}, {@code J4} and {@code J4}, with the same
 * verdict on both branches, which only the dump tells apart. With the fault of a member rendered without its place, the
 * second failed on the verdict: MVEL could not construct the fact. Until the switch-over this class was
 * {@code DrlTmsVouwpadWitnessTest}.
 */
class DrlTextFoldPathWitnessTest {

    @Test void the_deposit_of_a_filter_is_equal_on_both_branches() throws Exception {
        Path ir = TmsDocuments.document(TmsFilterDepositWitnessTest.MODEL,
                List.of(TmsFilterDepositWitnessTest.DEELNEMERS, TmsFilterDepositWitnessTest.ZWARE,
                        TmsFilterDepositWitnessTest.DRUKTE, TmsFilterDepositWitnessTest.ZORGVULDIG));
        List<String> withFact = new ArrayList<>();
        int n = 0;
        for (JsonNode caseDocument : List.of(TmsFilterDepositWitnessTest.case1(), TmsFilterDepositWitnessTest.case2(),
                TmsFilterDepositWitnessTest.case3())) {
            n++;
            if (BranchDumps.equal(ir, caseDocument, "zaak " + n).contains("feit FilterDeposit(")) withFact.add("zaak " + n);
        }
        assertEquals(List.of("zaak 1", "zaak 2"), withFact, "de zaken die een filterdeponering maken");
    }

    @Test void the_fault_of_a_member_is_equal_on_both_branches() throws Exception {
        Path ir = TmsTimelineDocuments.document(TmsInputFormWitnessTest.MODEL_E, TmsInputFormWitnessTest.RULES_E,
                List.of());
        List<String> instanties = new ArrayList<>(List.of(TmsInputFormWitnessTest.jutter("J1", List.of("V1", "V2")),
                TmsInputFormWitnessTest.jutter("J4", List.of("V6", "V5")),
                TmsInputFormWitnessTest.vondst("V1", "8", Map.of("jaartarief", TmsInputFormWitnessTest.JAARTARIEF_12),
                        TmsInputFormWitnessTest.KEPT_Q1),
                TmsInputFormWitnessTest.vondst("V2", "8", Map.of("tarief", TmsDocuments.numberValue("5")),
                        Map.of()),
                TmsInputFormWitnessTest.vondst("V5", "9", Map.of("tarief", TmsDocuments.numberValue("4")),
                        Map.of()),
                TmsInputFormWitnessTest.vondst("V6", "9", Map.of("tarief", TmsDocuments.numberValue("6")),
                        Map.of())));
        instanties.addAll(TmsInputFormWitnessTest.playingE());
        JsonNode caseDocument = TmsTimelineDocuments.caseDocument(TmsInputFormWitnessTest.DATE, instanties, List.of(consistent()));
        String dump = BranchDumps.equal(ir, caseDocument, "de zaak van deel E");
        assertEquals(3, Stream.of(dump.split("\n")).filter(r -> r.startsWith("feit MemberError(")).count(),
                () -> "drie lidfouten, op V2, V5 en V6:\n" + dump);
    }
}
