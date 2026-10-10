package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.ObjectDeletedEvent;
import org.kie.api.event.rule.ObjectInsertedEvent;
import org.kie.api.event.rule.ObjectUpdatedEvent;
import org.kie.api.event.rule.RuleRuntimeEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binderlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getaltype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getalwaarde;
import static dev.uchar.regelspraak.cases.TmsDocumenten.inconsistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMet;
import static dev.uchar.regelspraak.cases.TmsDocumenten.keuze;
import static dev.uchar.regelspraak.cases.TmsDocumenten.let;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.literaal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@code Let} (RUNTIME-SEMANTICS ch. 10.6), its {@code BinderRef} and an {@code IfThenElse} (ch. 10.2) on the
 * truth-maintenance branch, over a binder whose value another rule derives later: "De grondslag van een Jutter moet
 * berekend worden als zijn inkomen" and "De heffing van een Jutter moet berekend worden als: k is zijn grondslag; als k
 * groter is dan 10,00 dan 5,00, anders k gedeeld door zijn restpost". The division in the else branch is a §6.5 place
 * (a leeg divisor gives leeg and a §9.5 inconsistency, ch. 1.2) and a fault place (a numeric zero, ch. 1.4): it is
 * evaluated only where the branch is taken, and the binder is read, and its value evaluated, only where a branch reads
 * it. A read of a binder whose value is leeg ends the {@code Let} with leeg (clause 2).
 *
 * <p>The case input puts a grondslag of 6,00 beside an inkomen of 40,00, so in an order where the heffing fires before
 * the grondslag is derived it reads 6, takes the else branch and divides; at the fixpoint the grondslag is 40 and the
 * then branch writes 5,00. Case A: J1 (that transient, no restpost), J4 (inkomen 6, restpost 3: else, 2,00) and J6
 * (nothing: the grondslag stays leeg, the read raises the barrier, heffing leeg) — the transient deposit of J1 goes
 * again, so the case is consistent. Case B: J3 (inkomen 6, no restpost): else at the fixpoint, heffing leeg with the
 * §9.5 inconsistency of the heffing on J3. Case C: J2 (inkomen 40, grondslag 6, restpost 0): the transient division by
 * zero goes again, heffing 5,00. Case D: J5 (inkomen 6, restpost 0): the division by zero stands at the fixpoint. The
 * second document, with the binder q whose value divides and the nested Let of the saldo, is described at its row.
 *
 * <p><b>Measured</b> (2026-10-09, the reference interpreter's runner and the ASP runner on every order of both
 * documents): where the grondslag rule comes first, the reference interpreter passes cases A, B, C, E and F and faults on
 * D and G, "division by numeric zero". Where the reader comes first it reads the input grondslag 6: it keeps the
 * deposit of J1 ("MISMATCH: expected consistent, but the channel carries: heffing van een jutter × J1") and faults on
 * the transient division of J2 in cases C and E. That is the fault and the deposit of an intermediate state, which the
 * owner's decision of 2026-10-09 (PLAN §9, "Faults and deposits count at the fixpoint") does not count and which the
 * reference interpreter gives up with the card fout-op-tussenstand. The ASP runner passes A, B, C, E and F in every
 * order and faults on D and G ("viol("division by numeric zero", …)").
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on a copy of the branch at its claim, where the branch refused
 * the forms): every row was refused, "Excluded: tms-transpiler SKIP: regel: heffing van een jutter — een Let in een
 * waarde ('k is …'): buiten de proef van de truth-maintenance-tak", and the tarief as "een Let in een waarde ('q is
 * …')".
 */
class TmsLetWitnessTest {

    private static final String TWEE = getaltype(false, 2, null);

    static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(
            attribuut("inkomen", TWEE), attribuut("grondslag", TWEE), attribuut("restpost", TWEE),
            attribuut("heffing", TWEE)), List.of())), List.of());

    static final String GRONDSLAG = rekenregel("grondslag van een jutter", "BEREKENING", "Jutter", "grondslag",
            lezing("inkomen", "Jutter"), "null");

    static final String HEFFING = rekenregel("heffing van een jutter", "BEREKENING", "Jutter", "heffing",
            let("k", lezing("grondslag", "Jutter"), keuze(
                    vergelijking("greater", binderlezing("k"), literaal("10.00", TWEE)),
                    literaal("5.00", TWEE),
                    binair("dividedBy", binderlezing("k"), lezing("restpost", "Jutter"), TWEE), TWEE), TWEE), "null");

    static final List<String> REGELS = List.of(GRONDSLAG, HEFFING);

    static JsonNode zaakA() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J1", "Jutter", Map.of("inkomen", getalwaarde("40.00"), "grondslag", getalwaarde("6.00")),
                        List.of()),
                instantieMet("J4", "Jutter", Map.of("inkomen", getalwaarde("6.00"), "restpost", getalwaarde("3.00")),
                        List.of()),
                instantieMet("J6", "Jutter", Map.of(), List.of())),
                List.of(waarde("J1", "heffing", "5.00"), waarde("J4", "heffing", "2.00"), waarde("J6", "heffing", null),
                        consistent()));
    }

    static JsonNode zaakB() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J3", "Jutter", Map.of("inkomen", getalwaarde("6.00")), List.of())),
                List.of(waarde("J3", "heffing", null), inconsistent("heffing van een jutter", "J3")));
    }

    static JsonNode zaakC() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J2", "Jutter", Map.of("inkomen", getalwaarde("40.00"), "grondslag", getalwaarde("6.00"),
                        "restpost", getalwaarde("0.00")), List.of())),
                List.of(waarde("J2", "heffing", "5.00"), consistent()));
    }

    static JsonNode zaakD() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J5", "Jutter", Map.of("inkomen", getalwaarde("6.00"), "restpost", getalwaarde("0.00")),
                        List.of())),
                List.of(consistent()));
    }

    static final String J5_DOOR_NUL = "DelingDoorNul: deling door nul in de regel \"heffing van een jutter\": 6 gedeeld"
            + " door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void de_genomen_tak_rekent_en_de_andere_niet_in_elke_volgorde() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(REGELS)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            java.nio.file.Path ir = TmsDocumenten.document(MODEL, volgorde);
            for (JsonNode zaak : List.of(zaakA(), zaakB(), zaakC()))
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaakD(), DroolsBackend.Branch.EXEC_MODEL));
            for (int i = 0; i < 3; i++) verwacht.add(namen + " gespeeld");
            verwacht.add(namen + " " + J5_DOOR_NUL);
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- clause 3 and the barrier of a nested Let ----------------

    static final String MODEL_MET_TARIEF = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Jutter", List.of(
            attribuut("inkomen", TWEE), attribuut("grondslag", TWEE), attribuut("restpost", TWEE),
            attribuut("tarief", TWEE), attribuut("saldo", TWEE)), List.of())), List.of());

    /** "Het tarief van een Jutter moet berekend worden als: q is zijn inkomen gedeeld door zijn restpost; k is zijn
     *  grondslag; als k groter is dan 10,00 dan 5,00, anders q". */
    static final String TARIEF = rekenregel("tarief van een jutter", "BEREKENING", "Jutter", "tarief",
            let("q", binair("dividedBy", lezing("inkomen", "Jutter"), lezing("restpost", "Jutter"), TWEE),
                    let("k", lezing("grondslag", "Jutter"), keuze(
                            vergelijking("greater", binderlezing("k"), literaal("10.00", TWEE)),
                            literaal("5.00", TWEE), binderlezing("q"), TWEE), TWEE), TWEE), "null");

    /** "Het saldo van een Jutter moet berekend worden als: k is zijn grondslag; (x is zijn restpost; x) plus k". */
    static final String SALDO = rekenregel("saldo van een jutter", "BEREKENING", "Jutter", "saldo",
            let("k", lezing("grondslag", "Jutter"),
                    binair("plus", let("x", lezing("restpost", "Jutter"), binderlezing("x"), TWEE), binderlezing("k"),
                            TWEE), TWEE), "null");

    static JsonNode zaakE() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J2", "Jutter", Map.of("inkomen", getalwaarde("40.00"), "grondslag", getalwaarde("6.00"),
                        "restpost", getalwaarde("0.00")), List.of()),
                instantieMet("J7", "Jutter", Map.of("inkomen", getalwaarde("40.00")), List.of())),
                List.of(waarde("J2", "tarief", "5.00"), waarde("J2", "saldo", "40.00"), waarde("J7", "tarief", "5.00"),
                        waarde("J7", "saldo", "40.00"), consistent()));
    }

    static JsonNode zaakF() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J8", "Jutter", Map.of("inkomen", getalwaarde("6.00")), List.of())),
                List.of(waarde("J8", "tarief", null), waarde("J8", "saldo", "6.00"),
                        inconsistent("tarief van een jutter", "J8")));
    }

    static JsonNode zaakG() throws Exception {
        return TmsDocumenten.zaakOp(null, List.of(
                instantieMet("J5", "Jutter", Map.of("inkomen", getalwaarde("6.00"), "restpost", getalwaarde("0.00")),
                        List.of())),
                List.of(consistent()));
    }

    /** Clause 3 of ch. 10.6: the binder q, whose value divides, is read only on the else branch, so where the then
     *  branch is taken at the fixpoint its value is never evaluated: J2 (restpost 0) does not fault, and J7 (no
     *  restpost) does not deposit, although in an order where the tarief fires before the grondslag is derived J2
     *  reads the input grondslag 6, takes the else branch and divides by zero on the way. J8 (inkomen 6, no restpost)
     *  takes the else branch at the fixpoint: q is 6 divided by leeg, leeg with the §9.5 deposit (ch. 1.2), and its read
     *  ends the Let with leeg (clause 2). J5 (inkomen 6, restpost 0) reads q at the fixpoint and faults. The saldo
     *  holds a nested Let whose binder x reads the restpost: where it is leeg the read empties the nested Let alone,
     *  never the enclosing one (clause 2), and "plus" counts the leeg operand as 0 (ch. 1.2): J7 and J8 get their
     *  grondslag. */
    @Test void een_ongelezen_binder_rekent_niet_en_een_geneste_barriere_blijft_binnen() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(GRONDSLAG, TARIEF, SALDO))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            java.nio.file.Path ir = TmsDocumenten.document(MODEL_MET_TARIEF, volgorde);
            for (JsonNode zaak : List.of(zaakE(), zaakF())) {
                uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaak, DroolsBackend.Branch.EXEC_MODEL));
                verwacht.add(namen + " gespeeld");
            }
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speel(ir, zaakG(), DroolsBackend.Branch.EXEC_MODEL));
            verwacht.add(namen + " DelingDoorNul: deling door nul in de regel \"tarief van een jutter\": 6 gedeeld door 0"
                    + " — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)");
        }
        assertEquals(verwacht, uitkomsten);
    }

    /** Every order of the generated rules, on the beans of case A's J1 and case C's J2: the fixpoint has no fault and no
     *  deposit, and in at least one order the heffing took the else branch on the transient grondslag, divided by a
     *  leeg restpost (a deposit that went again) and by a zero (a fault that went again). */
    @Test void de_tussenstand_in_elke_volgorde_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, REGELS),
                DroolsBackend.Branch.EXEC_MODEL);
        int volgordes = 0, deponering = 0, fout = 0;
        for (List<Rule> volgorde : TmsDocumenten.permutaties(b.execModelTranspiler().model().getRules())) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of("inkomen", "40.00", "grondslag", "6.00"),
                    List.of());
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter",
                    Map.of("inkomen", "40.00", "grondslag", "6.00", "restpost", "0.00"), List.of());
            Feiten feiten = new Feiten();
            assertNull(TmsDocumenten.fixpunt(b, volgorde, List.of(j1, j2), sessie -> sessie.addEventListener(feiten)),
                    () -> "volgorde " + namen);
            assertEquals(feiten.ingevoegd, feiten.ingetrokken, () -> "foutfeiten en deponeringen in volgorde " + namen);
            TmsFoutWitnessTest.assertGetal("5.00", TmsDocumenten.lees(j1, "heffing"), "heffing van J1 in volgorde " + namen);
            TmsFoutWitnessTest.assertGetal("5.00", TmsDocumenten.lees(j2, "heffing"), "heffing van J2 in volgorde " + namen);
            if (feiten.deponeringen > 0) deponering++;
            if (feiten.fouten > 0) fout++;
            volgordes++;
        }
        assertTrue(deponering > 0, "geen volgorde deponeerde op de tijdelijke grondslag");
        assertTrue(fout > 0, "geen volgorde deelde door de tijdelijke nul");
        assertEquals(24, volgordes);
    }

    /** Counts the fault facts and the §9.5 inconsistencies a session inserts and withdraws. */
    static final class Feiten implements RuleRuntimeEventListener {
        int ingevoegd, ingetrokken, fouten, deponeringen;

        @Override public void objectInserted(ObjectInsertedEvent e) {
            String soort = e.getObject().getClass().getSimpleName();
            if (soort.equals("Fout")) { ingevoegd++; fouten++; }
            if (soort.equals("Inconsistentie")) { ingevoegd++; deponeringen++; }
        }

        @Override public void objectUpdated(ObjectUpdatedEvent e) {}

        @Override public void objectDeleted(ObjectDeletedEvent e) {
            String soort = e.getOldObject().getClass().getSimpleName();
            if (soort.equals("Fout") || soort.equals("Inconsistentie")) ingetrokken++;
        }
    }
}
