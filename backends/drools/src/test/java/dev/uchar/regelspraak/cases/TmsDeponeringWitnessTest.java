package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.predicaat;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The §6.5 deposit on the truth-maintenance branch wherever the reference interpreter makes it (RUNTIME-SEMANTICS ch. 1.2,
 * 5.6): every evaluated division of a filled dividend by a leeg divisor, whatever its operands, in a condition as well
 * as in a value, and in a condition whether it then holds or not, but only at the places the evaluation reaches
 * (ch. 5.10). Fixround 1 of the card, findings 1 and 2: until then a condition deposited nothing, and a value deposited
 * only where both operands were reads of an attribute or of a binder.
 *
 * <p>The document: a Jutter with {@code a} and {@code grond b} as input and "De b van een Jutter moet berekend worden
 * als zijn grond b", read by "Een Jutter is zwaar indien zijn a gedeeld door zijn b groter is dan 1", "Een Jutter is
 * zeer zwaar indien hij aan alle volgende voorwaarden voldoet: zijn a is groter dan 10; zijn a gedeeld door zijn b is
 * groter dan 1", "De heffing van een Jutter moet berekend worden als 5 gedeeld door zijn b", "De toeslag van een
 * Jutter moet geïnitialiseerd worden op 5 gedeeld door zijn b" and the consistency "verhoudingscontrole", "zijn a
 * gedeeld door zijn b is leeg of gevuld", which always holds, in every order of the rules. Case 1: J1 (a 5) and J3
 * (a 50), neither with a grond b: b stays leeg, so every division deposits where it is evaluated, except the one of
 * zeer zwaar on J1, whose composite settles on "a groter dan 10" before it (ch. 5.10). Case 2: J2 (a 5, grond b 10):
 * the deposits made while b is still leeg, in an order where a reader fires before the b is derived, go again.
 *
 * <p><b>Measured</b> (2026-10-09, on every order): the reference interpreter's runner passes case 1 in all 120 orders,
 * every deposit above included and none for zeer zwaar on J1; it passes case 2 where the b rule comes first, and in 80
 * of the 96 other orders keeps a deposit of the heffing or the toeslag made while b was leeg, the deposit of an
 * intermediate state that the owner's decision of 2026-10-09 (PLAN §9) does not count. The ASP runner passes case 2 in
 * every order, and carries every deposit of case 1 but the initialisation's, "expected inconsistent (… "toeslag van
 * een jutter" on "J1"), but the model carries: verhoudingscontrole × J1; …", where the reference interpreter deposits.
 *
 * <p><b>Red</b> on a3842004: "case: verwacht inconsistent (zwaar bij verhouding @ J1), werkelijk consistent; verwacht
 * inconsistent (heffing van een jutter @ J1), werkelijk consistent; verwacht inconsistent (toeslag van een jutter @ J1),
 * werkelijk consistent; verwacht inconsistent (verhoudingscontrole @ J1), werkelijk consistent; …".
 */
class TmsDeponeringWitnessTest {

    private static final String TWEE = getaltype(false, 2, null), GEHEEL = getaltype(true, 0, null);

    static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(
            attribuut("a", TWEE), attribuut("grond b", TWEE), attribuut("b", TWEE), attribuut("heffing", TWEE),
            attribuut("toeslag", TWEE)), List.of("zwaar", "zeer zwaar"))), List.of());

    private static final String A = lezing("a", "Jutter"), B = lezing("b", "Jutter");

    private static String deling(String teller) {
        return binair("dividedBy", teller, B, TWEE);
    }

    static final List<String> REGELS = List.of(
            rekenregel("b van een jutter", "BEREKENING", "Jutter", "b", lezing("grond b", "Jutter"), "null"),
            kenmerkregel("zwaar bij verhouding", "Jutter", "zwaar",
                    vergelijking("greater", deling(A), literaal("1", GEHEEL))),
            kenmerkregel("zeer zwaar bij verhouding", "Jutter", "zeer zwaar", samengesteld("all", 0, List.of(
                    vergelijking("greater", A, literaal("10", GEHEEL)),
                    vergelijking("greater", deling(A), literaal("1", GEHEEL))))),
            rekenregel("heffing van een jutter", "BEREKENING", "Jutter", "heffing", deling(literaal("5", GEHEEL)), "null"),
            rekenregel("toeslag van een jutter", "INITIALISATIE", "Jutter", "toeslag", deling(literaal("5", GEHEEL)),
                    "null"));

    static final String CONTROLE = TmsDocumenten.consistentie("verhoudingscontrole", "Jutter", samengesteld("atLeast", 1,
            List.of(predicaat("empty", deling(A)), predicaat("filled", deling(A)))));

    static JsonNode zaak1() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J1", "Jutter", Map.of("a", getalwaarde("5.00")), List.of()),
                instantieMet("J3", "Jutter", Map.of("a", getalwaarde("50.00")), List.of())),
                List.of(verwacht("J1", "zwaar", false), verwacht("J1", "zeer zwaar", false), waarde("J1", "heffing", null),
                        waarde("J1", "toeslag", null), verwacht("J3", "zwaar", false), verwacht("J3", "zeer zwaar", false),
                        inconsistent("zwaar bij verhouding", "J1"), inconsistent("heffing van een jutter", "J1"),
                        inconsistent("toeslag van een jutter", "J1"), inconsistent("verhoudingscontrole", "J1"),
                        inconsistent("zwaar bij verhouding", "J3"), inconsistent("zeer zwaar bij verhouding", "J3"),
                        inconsistent("heffing van een jutter", "J3"), inconsistent("toeslag van een jutter", "J3"),
                        inconsistent("verhoudingscontrole", "J3")));
    }

    static JsonNode zaak2() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J2", "Jutter", Map.of("a", getalwaarde("5.00"), "grond b", getalwaarde("10.00")), List.of())),
                List.of(verwacht("J2", "zwaar", false), waarde("J2", "heffing", "0.50"), waarde("J2", "toeslag", "0.50"),
                        consistent()));
    }

    @Test void elke_bereikte_deling_door_leeg_deponeert_ook_in_een_voorwaarde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(MODEL, volgorde, List.of(CONTROLE));
            for (JsonNode zaak : List.of(zaak1(), zaak2())) {
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(namen + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }
}
