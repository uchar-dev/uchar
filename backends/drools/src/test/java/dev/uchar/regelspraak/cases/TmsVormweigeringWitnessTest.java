package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An instance-set union where a value is read, which the truth-maintenance branch refuses for good, in the sentence of
 * the other two branches rather than as a form outside the trial. A union denotes the instances of a §5.7
 * concatenation and stands only as the single argument of a count (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS ch. 3.4d),
 * where the branch reads it; "Het unieaantal van een Jutter moet berekend worden als de vondsten van de Jutter en de
 * vondsten van de Jutter die zwaarder zijn dan 8", the union as the rule's value, has no reading.
 *
 * <p><b>Measured</b> (2026-10-09, on the golden jutterswet-vondstunie with the count around its union taken away): the
 * verifier refuses it, "WF-110 FAIL …: an instance-set union stands where nothing consumes it …"; the reference
 * interpreter faults, "EVALUATION FAULT: an instance-set union stands where a value is read: it denotes the INSTANCES
 * of a §5.7 concatenation, and only an instance count consumes them"; the ASP runner refuses it at translation in the
 * same words; the exec-model and the DRL-text branch refuse it as "instantie-unie op een waardeplek: zij duidt de
 * INSTANTIES van een §5.7-concatenatie aan en alleen een instantietelling verbruikt ze (WF-110 (a))".
 *
 * <p><b>Red</b> on 14d3bbb0: "but was: &lt;tms-transpiler SKIP: regel: unietelling — een InstanceSetUnion in een
 * waarde ('de vondsten van de Jutter en de vondsten van de Jutter die zwaarder zijn dan 8'): buiten de proef van de
 * truth-maintenance-tak&gt;".
 */
class TmsVormweigeringWitnessTest {

    private static String selectie(String filter, String bron) {
        return "{\"node\":\"InstanceSetBranch\",\"elementType\":\"Vondst\",\"sourceRole\":" + rolbinding("aanmelding",
                "vondst") + ",\"filter\":" + filter + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    @Test void een_unie_op_een_waardeplek_wordt_geweigerd_zoals_de_andere_takken_haar_weigeren() throws Exception {
        String snippet = "de vondsten van de Jutter en de vondsten van de Jutter die zwaarder zijn dan 8";
        String unie = "{\"node\":\"InstanceSetUnion\",\"branches\":[" + selectie("null", "de vondsten van de Jutter") + ","
                + selectie(TmsDocumenten.vergelijking("greater", TmsDocumenten.lezing("gewicht", "Vondst"),
                        TmsDocumenten.getal("8")), "de vondsten van de Jutter die zwaarder zijn dan 8")
                + "],\"source\":{\"snippet\":\"" + snippet + "\"}}";
        Path ir = TmsDocumenten.document(TmsFilterdeponeringWitnessTest.MODEL, List.of(
                rekenregel("unietelling", "BEREKENING", "Jutter", "unietelling", unie, "null")));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: unietelling — een InstanceSetUnion in een waarde ('" + snippet + "'): zij"
                + " duidt de instanties van een §5.7-concatenatie aan, en alleen een instantietelling verbruikt ze (WF-110"
                + " (a))", e.getMessage());
    }
}
