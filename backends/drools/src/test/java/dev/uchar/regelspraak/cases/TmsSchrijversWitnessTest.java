package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.tms.Fout;

import org.drools.model.Rule;
import org.junit.jupiter.api.Test;
import org.kie.api.event.rule.DefaultRuleRuntimeEventListener;
import org.kie.api.event.rule.ObjectInsertedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.predicaat;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two or more writers of one target on the truth-maintenance branch (RUNTIME-SEMANTICS ch. 5.7, "Divergence by two
 * writers of one target"; ch. 1.2g and 7.8 for a computed leeg; ch. 7.10 per concrete type).
 *
 * <p>The first document has three computing rules on the doel of a Vondst beside the rule that derives what two of
 * them read: "De tussen van een Vondst moet berekend worden als zijn a plus 1", "doel volgens tussen" (zijn tussen,
 * indien zijn tussen groter is dan 10), "doel volgens b" (zijn b, indien zijn b gevuld is) and "doel volgens c" (zijn
 * c, indien zijn tussen kleiner is dan 5). The doel has three writers, so it lies outside the standing domain of ch.
 * 7.8, and a writer that fires with a leeg value writes leeg (ch. 1.2g). V1 (a 20): one writer, doel 21. V2 (a 1, doel
 * 99 as input): "doel volgens c" fires with a leeg c and erases the input, doel leeg. V3 (a 20, b 21): two writers with
 * the same value, doel 21. V4 (a 7, b 7): the conditions of the first and the third writer exclude each other and
 * neither holds, doel 7. V5 (a 7, tussen 3 as input, b 5, c 9): on the input standing of the tussen "doel volgens c"
 * holds and writes 9 beside the 5 of "doel volgens b", a difference on an intermediate state only; at the fixpoint the
 * tussen is 8, and the doel 5. V6 (a 20, b 5): 21 against 5, the fault of ch. 5.7. V7 (a 1, b 7): 7 against a leeg
 * that "doel volgens c" writes, the fault as well. Every order of the four rules in the document, where the fault
 * names its writers in the order of that document; and a seeded sample of orders of the generated rules, at least one
 * of which must put the fault of V5 in the session on the intermediate state, from which truth maintenance withdraws
 * it.
 *
 * <p>The second document has the doel written on two sister subtypes and on their supertype: "doel van drijfgoed" (zijn
 * a), on Drijfgoed, "doel van wrakgoed" (zijn a plus 10), on Wrakgoed, "doel van een grote vondst" (5, indien zijn a
 * groter is dan 100), on Vondst, and "a van een vondst" (zijn c plus 1). Read per concrete type (IR-SPEC WF-079, ch.
 * 7.10) the two sister rules never apply to one instance, while the supertype's rule applies to every instance of
 * both. D1 (Drijfgoed, c 1): 2. W1 (Wrakgoed, c 1): 12. V1 (Vondst, c 1, doel 4 as input): no writer fires and the
 * input stands. V2 (Vondst, c 200): 5. D2 (Drijfgoed, c 200): 201 against 5, the fault. Every order of the four rules.
 *
 * <p>The third document has a computing rule beside a conditional initialisation, which the desugaring of ch. 7.7 does
 * not take, so the rule is outside the standing domain: "doel volgens b" (zijn b, indien zijn tussen groter is dan 10),
 * "standaarddoel" (geïnitialiseerd op 3, indien zijn c gevuld is) and the rule of the tussen. The guard of an
 * initialisation reads the target as the other writers leave it (ch. 5.4): a value another writer places switches it
 * off, and a leeg another writer writes leaves the target empty, input or not, so the initialisation fills it, and the
 * two writers then demand different values. V1 (a 20, b 7, c 1, tussen 3 as input): the initialisation fills the doel
 * on the intermediate state, and the rule's 7 displaces it at the fixpoint. V2 (a 20, doel 9 as input): the rule writes
 * leeg and erases the input; the initialisation's condition does not hold: leeg. V3 (a 1, c 1): only the
 * initialisation, 3. V4 (a 1, c 1, doel 9 as input): the input stands. V5 (a 20, c 1) and V6 (the same with doel 9 as
 * input): the rule writes leeg and the initialisation 3, the fault. Every order of the three rules, and a seeded sample
 * of orders of the generated rules.
 *
 * <p>Two initialisations on sister subtypes play, and one on their supertype beside them is refused (WF-079 per
 * concrete type). A rule on a member an object creation sets: {@code TmsCreatieInvoerWitnessTest}.
 *
 * <p>The red case {@code corpus/rood/jutterswet-dubbele-melding} gives on this branch the fault in the sentence its
 * {@code expectedFault} pins for Drools, the writers in the order of the document, as the reference interpreter names
 * them.
 *
 * <p><b>Measured in the reference interpreter and the ASP backend</b> (2026-10-09, on these documents in every order
 * of their rules, written out with their cases and run through {@code ref/bin/runner.exe} and
 * {@code backends/clingo/bin/clingo_runner.exe}). ref passed the cases that play in every order (6, 4 and 4
 * expectations) and faulted every case that faults here, in the sentence "two writers of one target disagree: rule
 * "doel volgens tussen" writes 21 and rule "doel volgens b" writes 5 to the attribute doel of V6, a Vondst", with the
 * writers in the order of the document in all 84 faulting runs and a leeg written as "leeg". clingo passed the second document's cases, faulted every case that faults here (UNSAT on its named
 * {@code viol} witness), and refused at translation the cases of the first and the third document that play, since
 * they put input on a target outside the standing domain ("the case supplies input for computed target "doel": ref
 * (like v0) overwrites it, which has no monotone encoding here"). On the red case ref says "two writers of one target
 * disagree: rule "melding op vinddag" writes 2026-03-04 and rule "melding op aanspoeldag" writes 2026-03-01 to the
 * attribute meldingsdatum of V, a Vondst". The document of the two initialisations on sister subtypes passed in ref
 * and in clingo in all six orders.
 *
 * <p><b>Red before the lowering</b> (measured 2026-10-09 on {@code f7bb0dd4}): the first and the second document were
 * refused, "tms-transpiler SKIP: regel: doel volgens tussen — twee of meer rekenende regels op één doel ('doel' van
 * Vondst: "doel volgens tussen", "doel volgens b", "doel volgens c"): hun v0-betekenis is de volgorde op de agenda
 * (RUNTIME-SEMANTICS ch. 7.7, WF-079)", the second with the writers "doel van drijfgoed", "doel van wrakgoed" and "doel
 * van een grote vondst"; the third, "tms-transpiler SKIP: regel: doel volgens b — een rekenende regel op een doel
 * buiten het staande domein ('doel' van Vondst), waar een berekende leeg het doel leeg maakt (RUNTIME-SEMANTICS ch.
 * 7.8): de truth-maintenance-tak draagt deze vorm nog niet"; the red case gave that refusal for its two rules instead of
 * the fault; the initialisations on sister subtypes were refused, "twee of meer initialisaties op één doel ('doel' van
 * Vondst: "standaarddoel van drijfgoed", "standaarddoel van wrakgoed"), voorwaardelijk of niet: …". Only the row on
 * the exec-model branch was green.
 *
 * <p><b>Red by mutation</b> (measured the same day, not committed). (1) The guard of the initialisation that does not
 * count a written leeg: V5 of the third document played, "Expected java.lang.RuntimeException to be thrown, but nothing
 * was thrown", in the document orders and in the generated ones. (2) A computed leeg that writes nothing outside the
 * standing domain: "volgorde [tussen van een vondst, doel volgens tussen, doel volgens b, doel volgens c] ==&gt;
 * expected: &lt;null&gt; but was: &lt;case: verwacht leeg, werkelijk 99&gt;", and four of the seven tests red. (3)
 * Without the rule that compares the writers' values, the write-back's own check gives the same sentence at the
 * fixpoint, and only the count of the generated rules tells: "expected: &lt;7&gt; but was: &lt;6&gt;".
 */
class TmsSchrijversWitnessTest {

    private static final String A = lezing("a", "Vondst");
    private static final String B = lezing("b", "Vondst");
    private static final String C = lezing("c", "Vondst");
    private static final String TUSSEN = lezing("tussen", "Vondst");

    private static final String TUSSENREGEL = rekenregel("tussen van een vondst", "BEREKENING", "Vondst", "tussen",
            binair("plus", A, getal("1")), "null");

    // ---------------- three computing rules on one target ----------------

    private static final List<String> DRIE = List.of(TUSSENREGEL,
            rekenregel("doel volgens tussen", "GELIJKSTELLING", "Vondst", "doel", TUSSEN,
                    vergelijking("greater", TUSSEN, getal("10"))),
            rekenregel("doel volgens b", "GELIJKSTELLING", "Vondst", "doel", B, predicaat("filled", B)),
            rekenregel("doel volgens c", "GELIJKSTELLING", "Vondst", "doel", C, vergelijking("less", TUSSEN, getal("5"))));

    private static final String MODEL = TmsDocumenten.model(List.of(TmsDocumenten.objecttype("Vondst",
            List.of(attribuut("a", GEHEEL), attribuut("b", GEHEEL), attribuut("c", GEHEEL), attribuut("tussen", GEHEEL),
                    attribuut("doel", GEHEEL)), List.of())), List.of());

    @Test void drie_schrijvers_in_elke_volgorde_van_het_document() throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(DRIE)) {
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(ir, drieZaak(), DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            assertEquals(conflict(namen, "doel volgens tussen", "21", "doel volgens b", "5", "doel", "V6, een Vondst"),
                    fout(ir, zaakVan("V6", "Vondst", Map.of("a", "20", "b", "5"))),
                    () -> "volgorde " + namen);
            assertEquals(conflict(namen, "doel volgens b", "7", "doel volgens c", "leeg", "doel", "V7, een Vondst"),
                    fout(ir, zaakVan("V7", "Vondst", Map.of("a", "1", "b", "7"))),
                    () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    @Test void de_exec_modeltak_geeft_dezelfde_eindtoestand() throws Exception {
        assertNull(CaseRunner.speelCase(TmsDocumenten.document(MODEL, DRIE), drieZaak(), DroolsBackend.Branch.EXEC_MODEL));
    }

    /** The rule of the tussen, its two standing rules, the three writers and the rule that compares their values: a
     *  sample of their orders, each over V1 to V5 and then over V6, written back without a case label. */
    @Test void steekproef_van_volgordes_van_de_gegenereerde_regels() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, DRIE),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(7, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        int opDeTussenstand = 0;
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261009L, 60)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = boon(b, Map.of("a", "20")), v2 = boon(b, Map.of("a", "1", "doel", "99")),
                    v3 = boon(b, Map.of("a", "20", "b", "21")), v4 = boon(b, Map.of("a", "7", "b", "7")),
                    v5 = boon(b, Map.of("a", "7", "tussen", "3", "b", "5", "c", "9"));
            List<Object> conflictOpV5 = new ArrayList<>();
            TmsDocumenten.vuur(b, volgorde, List.of(v1, v2, v3, v4, v5), s -> s.addEventListener(
                    new DefaultRuleRuntimeEventListener() {
                        @Override public void objectInserted(ObjectInsertedEvent e) {
                            if (e.getObject() instanceof Fout f && f.onderwerp() == v5
                                    && f.fout().getClass().getSimpleName().equals("Schrijversconflict"))
                                conflictOpV5.add(f);
                        }
                    }), s -> assertTrue(s.getObjects(o -> o instanceof Fout).isEmpty(), "een fout aan het fixpunt"));
            String waar = " in volgorde " + namen;
            assertGetal("21", v1, "doel", "V1" + waar);
            assertGetal(null, v2, "doel", "V2" + waar);
            assertGetal("21", v3, "doel", "V3" + waar);
            assertGetal("7", v4, "doel", "V4" + waar);
            assertGetal("5", v5, "doel", "V5" + waar);
            assertGetal("8", v5, "tussen", "V5" + waar);
            if (!conflictOpV5.isEmpty()) opDeTussenstand++;
            Object v6 = boon(b, Map.of("a", "20", "b", "5"));
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> TmsDocumenten.vuur(b, volgorde, List.of(v6), s -> {}, s -> {}), () -> "V6" + waar);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"doel volgens"
                    + " tussen\" schrijft 21 en de regel \"doel volgens b\" schrijft 5 in het attribuut doel van een Vondst"
                    + " (Vondst@" + Integer.toHexString(System.identityHashCode(v6)) + "); divergentie is een fout"
                    + " (RUNTIME-SEMANTICS ch. 5.7)", e.getMessage(), "V6" + waar);
        }
        assertTrue(opDeTussenstand > 0, "geen volgorde zette de fout van V5 op de tussenstand in de sessie");
    }

    private static JsonNode drieZaak() throws Exception {
        return TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("a", "20"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("a", "1", "doel", "99"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of("a", "20", "b", "21"), List.of(), Map.of()),
                        instantie("V4", "Vondst", Map.of("a", "7", "b", "7"), List.of(), Map.of()),
                        instantie("V5", "Vondst", Map.of("a", "7", "tussen", "3", "b", "5", "c", "9"), List.of(),
                                Map.of())),
                List.of(waarde("V1", "doel", "21"), waarde("V2", "doel", null), waarde("V3", "doel", "21"),
                        waarde("V4", "doel", "7"), waarde("V5", "doel", "5"), waarde("V5", "tussen", "8")));
    }

    // ---------------- writers on sister subtypes and their supertype ----------------

    private static final List<String> ZUSTERS = List.of(
            rekenregel("doel van drijfgoed", "GELIJKSTELLING", "Drijfgoed", "doel", lezing("a", "Drijfgoed"), "null"),
            rekenregel("doel van wrakgoed", "GELIJKSTELLING", "Wrakgoed", "doel",
                    binair("plus", lezing("a", "Wrakgoed"), getal("10")), "null"),
            rekenregel("doel van een grote vondst", "GELIJKSTELLING", "Vondst", "doel", getal("5"),
                    vergelijking("greater", A, getal("100"))),
            rekenregel("a van een vondst", "BEREKENING", "Vondst", "a", binair("plus", C, getal("1")), "null"));

    private static final String ZUSTERMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("a", GEHEEL), attribuut("c", GEHEEL),
                    attribuut("doel", GEHEEL)), List.of()),
            TmsDocumenten.subtype("Drijfgoed", "Vondst", List.of(), List.of()),
            TmsDocumenten.subtype("Wrakgoed", "Vondst", List.of(), List.of())), List.of());

    @Test void schrijvers_op_zustersubtypes_in_elke_volgorde_van_het_document() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("D1", "Drijfgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instantie("W1", "Wrakgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instantie("V1", "Vondst", Map.of("c", "1", "doel", "4"), List.of(), Map.of()),
                        instantie("V2", "Vondst", Map.of("c", "200"), List.of(), Map.of())),
                List.of(waarde("D1", "doel", "2"), waarde("W1", "doel", "12"), waarde("V1", "doel", "4"),
                        waarde("V2", "doel", "5")));
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(ZUSTERS)) {
            Path ir = TmsDocumenten.document(ZUSTERMODEL, volgorde);
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            assertEquals(conflict(namen, "doel van drijfgoed", "201", "doel van een grote vondst", "5", "doel",
                            "D2, een Drijfgoed"),
                    fout(ir, zaakVan("D2", "Drijfgoed", Map.of("c", "200"))),
                    () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(24, volgordes);
    }

    // ---------------- initialisations on sister subtypes ----------------

    private static final List<String> ZUSTERINITIALISATIES = List.of(
            rekenregel("standaarddoel van drijfgoed", "INITIALISATIE", "Drijfgoed", "doel", lezing("a", "Drijfgoed"),
                    "null"),
            rekenregel("standaarddoel van wrakgoed", "INITIALISATIE", "Wrakgoed", "doel", getal("7"), "null"),
            rekenregel("a van een vondst", "BEREKENING", "Vondst", "a", binair("plus", C, getal("1")), "null"));

    /** "Het doel van een Drijfgoed moet geïnitialiseerd worden op zijn a", "Het doel van een Wrakgoed moet
     *  geïnitialiseerd worden op 7" and the rule of the a: two initialisations on sister subtypes never apply to one
     *  instance (IR-SPEC WF-079, its initialisation arm, read per concrete type). D1 (Drijfgoed, c 1): 2, the a derived
     *  after the initialisation may have read its input standing. W1 (Wrakgoed): 7. D2 (Drijfgoed, doel 5 as input): 5.
     *  V1 (Vondst): no initialisation, leeg. Every order of the three rules. An initialisation on the supertype beside
     *  them writes every instance of both, and is refused with the one on the subtype that comes first. */
    @Test void initialisaties_op_zustersubtypes_in_elke_volgorde_van_het_document() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("D1", "Drijfgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instantie("W1", "Wrakgoed", Map.of("c", "1"), List.of(), Map.of()),
                        instantie("D2", "Drijfgoed", Map.of("c", "1", "doel", "5"), List.of(), Map.of()),
                        instantie("V1", "Vondst", Map.of("c", "1"), List.of(), Map.of())),
                List.of(waarde("D1", "doel", "2"), waarde("W1", "doel", "7"), waarde("D2", "doel", "5"),
                        waarde("V1", "doel", null)));
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(ZUSTERINITIALISATIES)) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(TmsDocumenten.document(ZUSTERMODEL, volgorde), zaak,
                    DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            volgordes++;
        }
        assertEquals(6, volgordes);
        List<String> metSupertype = new ArrayList<>(ZUSTERINITIALISATIES);
        metSupertype.add(rekenregel("standaarddoel van een vondst", "INITIALISATIE", "Vondst", "doel", getal("3"), "null"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class, () -> DroolsBackend.build(
                TmsDocumenten.document(ZUSTERMODEL, metSupertype), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertEquals("tms-transpiler SKIP: regel: standaarddoel van drijfgoed — twee of meer initialisaties op één doel"
                + " ('doel' van Vondst: \"standaarddoel van drijfgoed\", \"standaarddoel van een vondst\"), voorwaardelijk of"
                + " niet: waar ze allebei gelden rangschikt niets ze en bepaalt het fixpunt de waarde niet"
                + " (RUNTIME-SEMANTICS ch. 5.4, WF-079)", e.getMessage());
    }

    // ---------------- a computing rule beside a conditional initialisation ----------------

    private static final List<String> NAAST_INITIALISATIE = List.of(TUSSENREGEL,
            rekenregel("doel volgens b", "GELIJKSTELLING", "Vondst", "doel", B,
                    vergelijking("greater", TUSSEN, getal("10"))),
            rekenregel("standaarddoel", "INITIALISATIE", "Vondst", "doel", getal("3"), predicaat("filled", C)));

    @Test void naast_een_voorwaardelijke_initialisatie_in_elke_volgorde_van_het_document() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(
                List.of(instantie("V1", "Vondst", Map.of("a", "20", "b", "7", "c", "1", "tussen", "3"), List.of(),
                                Map.of()),
                        instantie("V2", "Vondst", Map.of("a", "20", "doel", "9"), List.of(), Map.of()),
                        instantie("V3", "Vondst", Map.of("a", "1", "c", "1"), List.of(), Map.of()),
                        instantie("V4", "Vondst", Map.of("a", "1", "c", "1", "doel", "9"), List.of(), Map.of())),
                List.of(waarde("V1", "doel", "7"), waarde("V2", "doel", null), waarde("V3", "doel", "3"),
                        waarde("V4", "doel", "9")));
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(NAAST_INITIALISATIE)) {
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            assertNull(CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE), () -> "volgorde " + namen);
            for (String label : List.of("V5", "V6")) {
                Map<String, String> invoer = label.equals("V5") ? Map.of("a", "20", "c", "1")
                        : Map.of("a", "20", "c", "1", "doel", "9");
                assertEquals(conflict(namen, "doel volgens b", "leeg", "standaarddoel", "3", "doel", label + ", een Vondst"),
                        fout(ir, zaakVan(label, "Vondst", invoer)),
                        () -> label + " in volgorde " + namen);
            }
            volgordes++;
        }
        assertEquals(6, volgordes);
    }

    /** The rule of the tussen, its two standing rules, the computing rule, the initialisation and the rule that
     *  compares the values: a sample of their orders over V1 to V4, written back without a case label, and over V5. */
    @Test void naast_een_voorwaardelijke_initialisatie_in_een_steekproef_van_gegenereerde_volgordes() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsDocumenten.document(MODEL, NAAST_INITIALISATIE),
                DroolsBackend.Branch.TRUTH_MAINTENANCE);
        List<Rule> gegenereerd = b.tmsTranspiler().model().getRules();
        assertEquals(6, gegenereerd.size(), () -> "gegenereerde regels: " + gegenereerd.stream().map(Rule::getName).toList());
        for (List<Rule> volgorde : TmsDocumenten.steekproef(gegenereerd, 20261009L, 60)) {
            List<String> namen = volgorde.stream().map(Rule::getName).toList();
            Object v1 = boon(b, Map.of("a", "20", "b", "7", "c", "1", "tussen", "3")),
                    v2 = boon(b, Map.of("a", "20", "doel", "9")), v3 = boon(b, Map.of("a", "1", "c", "1")),
                    v4 = boon(b, Map.of("a", "1", "c", "1", "doel", "9"));
            TmsDocumenten.vuur(b, volgorde, List.of(v1, v2, v3, v4), s -> {}, s -> {});
            String waar = " in volgorde " + namen;
            assertGetal("7", v1, "doel", "V1" + waar);
            assertGetal(null, v2, "doel", "V2" + waar);
            assertGetal("3", v3, "doel", "V3" + waar);
            assertGetal("9", v4, "doel", "V4" + waar);
            Object v5 = boon(b, Map.of("a", "20", "c", "1"));
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> TmsDocumenten.vuur(b, volgorde, List.of(v5), s -> {}, s -> {}), () -> "V5" + waar);
            assertEquals("geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"doel volgens"
                    + " b\" schrijft leeg en de regel \"standaarddoel\" schrijft 3 in het attribuut doel van een Vondst"
                    + " (Vondst@" + Integer.toHexString(System.identityHashCode(v5)) + "); divergentie is een fout"
                    + " (RUNTIME-SEMANTICS ch. 5.7)", e.getMessage(), "V5" + waar);
        }
    }

    // ---------------- the red case of the corpus ----------------

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path DUBBELE_MELDING = Path.of("..", "..", "corpus", "rood", "jutterswet-dubbele-melding");

    @Test void het_rode_geval_geeft_de_fout_met_de_zin_van_ref() throws Exception {
        List<Path> zaken;
        try (var s = Files.list(DUBBELE_MELDING)) {
            zaken = s.filter(p -> p.getFileName().toString().matches("case.*\\.json")).sorted().toList();
        }
        assertEquals(2, zaken.size());
        for (Path z : zaken) {
            JsonNode c = TmsDocumenten.JSON.readTree(Files.readString(z));
            String pin = c.path("expectedFault").path("drools").asText();
            assertTrue(pin.startsWith("twee schrijvers van één doel"), () -> z + ": " + pin);
            String zin = fout(DUBBELE_MELDING.resolve("ir.json"), c);
            assertTrue(zin.contains(pin), () -> z + ": de zin\n" + zin + "\nbevat de pin niet\n" + pin);
        }
    }

    // ---------------- helpers ----------------

    /** The sentence of RUNTIME-SEMANTICS ch. 5.7 for the two writers {@code a} and {@code b}, in the order of the
     *  document whose rules are {@code namen}. */
    private static String conflict(List<String> namen, String a, String waardeA, String b, String waardeB, String lid,
                                   String object) {
        boolean aEerst = namen.indexOf(a) < namen.indexOf(b);
        String eerste = aEerst ? a : b, tweede = aEerst ? b : a;
        String eersteWaarde = aEerst ? waardeA : waardeB, tweedeWaarde = aEerst ? waardeB : waardeA;
        return "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel \"" + eerste
                + "\" schrijft " + eersteWaarde + " en de regel \"" + tweede + "\" schrijft " + tweedeWaarde
                + " in het attribuut " + lid + " van " + object + "; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)";
    }

    /** The message of the fault the replay of {@code zaak} on this branch ends in. */
    private static String fout(Path ir, JsonNode zaak) {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CaseRunner.speelCase(ir, zaak, DroolsBackend.Branch.TRUTH_MAINTENANCE));
        return e.getMessage();
    }

    /** A case of one instance whose replay faults: its expectation is never read. */
    private static JsonNode zaakVan(String label, String type, Map<String, String> invoer) throws Exception {
        return TmsDocumenten.zaak(List.of(instantie(label, type, invoer, List.of(), Map.of())),
                List.of(waarde(label, "doel", "0")));
    }

    private static Object boon(DroolsBackend.Build b, Map<String, String> getallen) throws Exception {
        return TmsDocumenten.boon(b.beanLoader(), "Vondst", getallen, List.of());
    }

    private static void assertGetal(String verwacht, Object boon, String attribuut, String wat) throws Exception {
        Object werkelijk = TmsDocumenten.lees(boon, attribuut);
        if (verwacht == null) assertNull(werkelijk, wat + " " + attribuut);
        else assertEquals(0, new java.math.BigDecimal(verwacht).compareTo((java.math.BigDecimal) werkelijk),
                () -> wat + " " + attribuut + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
